package com.lsgio.termuxpinger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.lsgio.termuxpinger.R;
import com.lsgio.termuxpinger.models.AddressRecord;

import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {
    public interface OnPingClickListener {
        void onPingClick(AddressRecord address);
        void onDeleteClick(int position, AddressRecord address);
        void onLogClick(AddressRecord address);
    }

    private final List<AddressRecord> addressList;
    private final OnPingClickListener listener;

    public AddressAdapter(List<AddressRecord> addressList, OnPingClickListener listener) {
        this.addressList = addressList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ip_address, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AddressRecord ip = addressList.get(position);
        holder.textLabel.setText(ip.getLabel());
        holder.textAddress.setText(ip.getAddress());
        holder.buttonPing.setOnClickListener(v -> listener.onPingClick(ip));
        MaterialButton buttonDelete = holder.itemView.findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(v -> listener.onDeleteClick(holder.getAdapterPosition(), ip));
        MaterialButton buttonLog = holder.itemView.findViewById(R.id.buttonLog);
        buttonLog.setOnClickListener(v -> listener.onLogClick(ip));
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    public void addIpAddress(AddressRecord ip) {
        addressList.add(ip);
        notifyItemInserted(addressList.size() - 1);
    }

    public void removeIpAddress(int position) {
        addressList.remove(position);
        notifyItemRemoved(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textLabel, textAddress;
        MaterialButton buttonPing;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textLabel = itemView.findViewById(R.id.textLabel);
            textAddress = itemView.findViewById(R.id.textAddress);
            buttonPing = itemView.findViewById(R.id.buttonPing);
        }
    }
} 