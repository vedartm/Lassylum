package com.venky97vp.android.lassylum;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class AlertActivity extends AppCompatActivity {

    private static final String TAG = "AlertActivity";
    double latitude;
    double longitude;
    LocationListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String json = sharedPrefs.getString(TAG, null);
        Type type = new TypeToken<ArrayList<Contact>>() {
        }.getType();
        if (gson.fromJson(json, type) != null) {
            SelectedContacts.selected = gson.fromJson(json, type);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                +WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                +WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        Log.d("AlertActivity", " : Started");

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

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        locationManager.requestLocationUpdates("gps", 5000, 0, listener);

        Location myLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        longitude = myLocation.getLongitude();
        latitude = myLocation.getLatitude();

        if (SelectedContacts.selected.size() > 0) {
            sendSMSfromList();
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.siren_noise);
            mediaPlayer.start();
        } else {
            Toast.makeText(this, "Add contacts to send message", Toast.LENGTH_LONG).show();
        }
    }

    private void sendSMSfromList() {
        for (Contact contact : SelectedContacts.selected) {
            sendSMS(contact.getNumber(), getMessage(latitude, longitude));
            //Toast.makeText(this,getMessage(latitude, longitude),Toast.LENGTH_LONG).show();
            Log.d(TAG, "sendSMSfromList: " + getMessage(latitude, longitude));
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        setContentView(R.layout.activity_alert_success);
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        setContentView(R.layout.activity_alert_failed);
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        setContentView(R.layout.activity_alert_failed);
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        setContentView(R.layout.activity_alert_failed);
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        setContentView(R.layout.activity_alert_failed);
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        setContentView(R.layout.activity_alert_success);
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        setContentView(R.layout.activity_alert_failed);
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
//        TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
//        String mPhoneNumber = tMgr.get();
        sms.sendTextMessage(phoneNumber ,null, message, sentPI, deliveredPI);
    }


    String getMessage(double latitude, double longitude) {
        String message = "I might be in danger. My location is\n";
//        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        List<Address> addressList;
//        try {
//            addressList = geocoder.getFromLocation(latitude, longitude, 1);
//            if (addressList.size() > 0) {
//
//                String address1 = addressList.get(0).getAddressLine(0);
//                String city = addressList.get(0).getLocality();
//                String state = addressList.get(0).getAdminArea();
//                String postalCode = addressList.get(0).getPostalCode();
//                String knownName = addressList.get(0).getFeatureName();
//                message += address1 + "\n" + city + "\n" + state + "\n" + postalCode;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return message + "Latitude = " + latitude + "\nLongitude = " + longitude + "\n\"http://maps.google.com?q=" + latitude + "," + longitude + "\"";
    }
}
