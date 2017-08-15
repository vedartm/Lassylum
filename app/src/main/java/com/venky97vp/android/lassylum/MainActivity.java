package com.venky97vp.android.lassylum;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsManager;
import android.text.Selection;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Handler;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final static int LOCATION_REQUEST_CODE = 108;
    private static final String TAG = "savedContacts";
    LocationListener listener;
    double longitude;
    double latitude;
    NotificationCompat.Builder builder;
    RecyclerView recyclerView;
    ContactsRecyclerAdapter mAdapter;
    AutoCompleteTextView textView;
    String[] PERMISSIONS = {Manifest.permission.READ_CONTACTS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS};

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
        }

        customNotification();

        final String PREFS_NAME = "MyPrefsFile";

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        if (settings.getBoolean("my_first_time", true)) {
            //the app is being launched for first time, do something
            Log.d("Comments", "First time");

            // first time task
            new AlertDialog.Builder(this)
                    .setPositiveButton("Load", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setContactList();
                        }
                    })
                    .setCancelable(false)
                    .setTitle("Load Contacts")
                    .setMessage("Load your contacts for easy selection")
                    .show();

            // record the fact that the app has been started at least once
            settings.edit().putBoolean("my_first_time", false).apply();
        } else {
            loadAllContacts();
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String json = sharedPrefs.getString(TAG, null);
        Type type = new TypeToken<ArrayList<Contact>>() {
        }.getType();
        if (gson.fromJson(json, type) != null) {
            SelectedContacts.selected = gson.fromJson(json, type);
        }

        checkIfList();

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };

        recyclerView = (RecyclerView) findViewById(R.id.contact_list);

        mAdapter = new ContactsRecyclerAdapter(this, SelectedContacts.selected);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        ImageButton button = (ImageButton) findViewById(R.id.send_sms);
        ImageButton addContacts = (ImageButton) findViewById(R.id.add_button);
        Button clear = (Button) findViewById(R.id.clear);
        button.setOnClickListener(this);
        addContacts.setOnClickListener(this);
        clear.setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                } else {
                    //Toast.makeText(getApplicationContext(),"Accept location permission so that people can reach you",Toast.LENGTH_LONG).show();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    void checkIfList() {
        TextView addContactText = (TextView) findViewById(R.id.not_added_text);
        if (SelectedContacts.selected.size() != 0) {
            addContactText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.send_sms) {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_REQUEST_CODE);
                }
            } else {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestLocationUpdates("gps", 5000, 0, listener);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    //buildAlertMessageNoGps();
                } else {
                    if(SelectedContacts.selected.size()>0) {
                        Location myLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                        longitude = myLocation.getLongitude();
                        latitude = myLocation.getLatitude();

                        //Vibrator vibrator = (Vibrator) this.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                        //vibrator.vibrate(500);

                        //CustomNotification();
                        new AlertDialog.Builder(this)
                                .setTitle("Send SMS")
                                .setMessage("Are you sure you want to send your location?")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        //sendSMSfromList();
                                        Intent intent = new Intent(getApplicationContext(), AlertActivity.class);
                                        intent.putExtra("lat", latitude);
                                        intent.putExtra("long", longitude);
                                        startActivity(intent);
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // do nothing
                                    }
                                })
                                .show();
                    }else{
                        Toast.makeText(this,"Add contacts to send message",Toast.LENGTH_LONG).show();
                    }
                }
            }
        } else if (v.getId() == R.id.add_button) {
            //SelectedContacts.selected.add(0,);
            String s = textView.getText().toString();
            try {
                String number = s.substring(s.indexOf(':') + 1);
                String name = s.substring(0, s.indexOf(':'));
                Log.d("List", name + "-" + number);
                Contact temp = new Contact(name, number);
                if (!searchIfPresent(temp)) {
                    SelectedContacts.selected.add(temp);
                    mAdapter.notifyItemInserted(SelectedContacts.selected.size() - 1);
                    saveSharedPref();
                    checkIfList();
                } else {
                    Toast.makeText(this, "Already Added in the List", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Search for a valid contact", Toast.LENGTH_LONG).show();
            }
            textView.setText("");
            //Toast.makeText(this,SelectedContacts.selected.get(1).getName(),Toast.LENGTH_LONG).show();
        } else {
            clearSavedPrefs();
        }
    }

    private void clearSavedPrefs() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.clear().apply();
        Log.d("Preferences", "Cleared");
    }

    public void customNotification() {
        // Using RemoteViews to bind custom layouts into Notification
        RemoteViews remoteViews = new RemoteViews(getPackageName(),
                R.layout.custom_notification);

        // Set Notification Title
        String strtitle = getString(R.string.app_name);
        // Set Notification Text

        // Open NotificationView Class on Notification Click
        Intent intent = new Intent(this, AlertActivity.class);
        Intent intent1 = new Intent(this, MainActivity.class);
        // Send data to NotificationView Class
        intent.putExtra("title", strtitle);

        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pIntent1 = PendingIntent.getActivity(this, 0, intent1,
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setOngoing(true)
                .setContentIntent(pIntent1)
                .setContent(remoteViews);

        remoteViews.setTextViewText(R.id.title, getString(R.string.app_name));
        //remoteViews.setOnClickPendingIntent();

        remoteViews.setOnClickPendingIntent(R.id.notification_button,pIntent);

        // Create Notification Manager
        NotificationManager notificationmanager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Build Notification with Notification Manager
        notificationmanager.notify(0, builder.build());
    }

    void setContactList() {
        ArrayList<Contact> contactsList = new ArrayList<>();
        try {
            String phoneNumber = "";
            ContentResolver cr = getBaseContext().getContentResolver();
            Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            if (cur != null && cur.getCount() > 0) {
                Log.i("AutocompleteContacts", "Reading   contacts........");
                int k = 0;
                String name = "";
                while (cur.moveToNext()) {
                    String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                    name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    //Check contact have phone number
                    if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                        //Create query to get phone number by contact id
                        Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                        int j = 0;
                        assert pCur != null;
                        while (pCur
                                .moveToNext()) {
                            if (j == 0) {
                                phoneNumber = "" + pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                contactsList.add(new Contact(name, phoneNumber));
                                j++;
                                k++;
                            }
                        }  // End while loop
                        pCur.close();
                    } // End if
                }  // End while loop
            }
            assert cur != null;
            cur.close();
        } catch (Exception e) {
            Log.i("AutocompleteContacts", "Exception : " + e);
        }

        saveAllContacts(contactsList);
        ContactsAdapter contactsAdapter = new ContactsAdapter(this, R.layout.activity_main, R.id.contact_name, contactsList);
        textView = (AutoCompleteTextView) findViewById(R.id.add_contacts);
        textView.setAdapter(contactsAdapter);
    }

    public void loadAllContacts() {
        ArrayList<Contact> contactsList = null;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String json = sharedPrefs.getString("all_contacts", null);
        Type type = new TypeToken<ArrayList<Contact>>() {
        }.getType();
        if (gson.fromJson(json, type) != null) {
            contactsList = gson.fromJson(json, type);
            ContactsAdapter contactsAdapter = new ContactsAdapter(this, R.layout.activity_main, R.id.contact_name, contactsList);
            textView = (AutoCompleteTextView) findViewById(R.id.add_contacts);
            textView.setAdapter(contactsAdapter);
        } else {
            Log.e("Error", "Loading from Preferences");
        }
    }

    private boolean searchIfPresent(Contact toCheck) {
        for (Contact contact : SelectedContacts.selected) {
            if (contact.getNumber().equals(toCheck.getNumber())) {
                return true;
            }
        }
        return false;
    }

    void saveSharedPref() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Gson gson = new Gson();

        String json = gson.toJson(SelectedContacts.selected);

        editor.putString(TAG, json);
        editor.apply();
    }

    void saveAllContacts(ArrayList<Contact> contacts) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Gson gson = new Gson();

        String json = gson.toJson(contacts);

        editor.putString("all_contacts", json);
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.refresh:
                //new AsyncCaller().execute((Void[])null);
//                this.runOnUiThread(new Runnable() {
//                    public void run() {
//                        new AsyncCaller().execute((Void[])null);
//                    }
//                });
                setContactList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class AsyncCaller extends AsyncTask<Void, Void, Void> {
        ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);

        AsyncCaller(){

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pdLoading.setMessage("\tRefreshing contacts...");
            //pdLoading.show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            setContactList();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            //pdLoading.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveSharedPref();
    }
}
