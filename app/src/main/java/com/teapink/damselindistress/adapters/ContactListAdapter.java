package com.teapink.damselindistress.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.teapink.damselindistress.R;
import com.teapink.damselindistress.models.Contact;

import java.util.ArrayList;

public class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.MyViewHolder> {

    private Context context;
    private ArrayList<Contact> contactArrayList;

    public ContactListAdapter(Context context, ArrayList<Contact> contactArrayList) {
        this.context = context;
        this.contactArrayList = contactArrayList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_contact_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final Contact contact = contactArrayList.get(position);
        holder.contactName.setText(contact.getName());
        holder.contactPhone.setText(contact.getPhone());
    }

    @Override
    public int getItemCount() {
        return contactArrayList == null ? 0 : contactArrayList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView contactName, contactPhone;

        MyViewHolder(View itemView) {
            super(itemView);
            contactName = (TextView) itemView.findViewById(R.id.contactName);
            contactPhone = (TextView) itemView.findViewById(R.id.contactPhone);
        }
    }
}