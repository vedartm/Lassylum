package com.venky97vp.android.lassylum;

/**
 * Created by venky on 30-04-2017.
 */

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akshay on 1/2/15.
 */
public class ContactsAdapter extends ArrayAdapter<Contact> {

    Context context;
    int resource, textViewResourceId;
    List<Contact> items, tempItems, suggestions;

    public ContactsAdapter(Context context, int resource, int textViewResourceId, List<Contact> items) {
        super(context, resource, textViewResourceId, items);
        this.context = context;
        this.resource = resource;
        this.textViewResourceId = textViewResourceId;
        this.items = items;
        tempItems = new ArrayList<Contact>(items); // this makes the difference.
        suggestions = new ArrayList<Contact>();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_contact, parent, false);
        }
        Contact people = items.get(position);
        if (people != null) {
            TextView lblNumber = (TextView) view.findViewById(R.id.contact_number);
            TextView lblName = (TextView) view.findViewById(R.id.contact_name);
            if (lblName != null) {
                lblName.setText(people.getName());
                lblNumber.setText(people.getNumber());
            }
        }
        return view;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return nameFilter;
    }

    /**
     * Custom Filter implementation for custom suggestions we provide.
     */
    Filter nameFilter = new Filter() {
        @Override
        public CharSequence convertResultToString(Object resultValue) {
            Contact contact = (Contact) resultValue;
            return contact.getName()+":"+contact.getNumber();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint != null) {
                suggestions.clear();
                for (Contact people : tempItems) {
                    if (people.getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
                        suggestions.add(people);
                    }
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = suggestions;
                filterResults.count = suggestions.size();
                return filterResults;
            } else {
                return new FilterResults();
            }
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            List<Contact> filterList = (ArrayList<Contact>) results.values;
            if (results.count > 0) {
                clear();
                for (Contact people : filterList) {
                    add(people);
                    notifyDataSetChanged();
                }
            }
        }
    };
}