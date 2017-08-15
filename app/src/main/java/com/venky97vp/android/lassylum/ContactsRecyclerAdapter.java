package com.venky97vp.android.lassylum;

/**
 * Created by venky on 01-05-2017.
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class ContactsRecyclerAdapter extends RecyclerView.Adapter<ContactsRecyclerAdapter.MyViewHolder> {

    private List<Contact> moviesList;
    private Context mContext;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView name, number;
        ImageButton remove;

        public MyViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.contact_name);
            number = (TextView) view.findViewById(R.id.contact_number);
            remove = (ImageButton) view.findViewById(R.id.remove_button);
        }
    }

    public ContactsRecyclerAdapter(Context mContext, List<Contact> moviesList) {
        this.mContext = mContext;
        this.moviesList = moviesList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_contact_selected, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        final Contact movie = moviesList.get(position);
        holder.name.setText(movie.getName());
        holder.number.setText(movie.getNumber());
        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moviesList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position,moviesList.size());
                Toast.makeText(mContext,"Removed",Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return moviesList.size();
    }
}