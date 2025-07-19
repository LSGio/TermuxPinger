package com.lsgio.termuxpinger;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lsgio.termuxpinger.adapters.IpAddressAdapter;
import com.lsgio.termuxpinger.models.IpAddressRecord;
import com.lsgio.termuxpinger.utils.TermuxConstants;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private IpAddressAdapter adapter;
    private ArrayList<IpAddressRecord> ipList;
    private static final String KEY_INITIAL_SETUP_DONE = "isInitialSetupDone";
    private static final String KEY_IP_LIST_JSON = "ipListJson";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load the list from SharedPreferences
        String json = PreferenceManager.getDefaultSharedPreferences(this).getString(KEY_IP_LIST_JSON, null);
        if (json != null) {
            ipList = new ArrayList<>(IpAddressRecord.fromJsonString(json));
        } else {
            ipList = new ArrayList<>();
        }
        // Add an example entry if you want it to appear on first launch
        boolean isFirstLaunch = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KEY_INITIAL_SETUP_DONE, true);
        if (isFirstLaunch) {
            // Use custom layout for the dialog
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_initial_setup, null);
            CheckBox checkBox = dialogView.findViewById(R.id.checkboxShowNextLaunch);

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Initial Setup")
                    .setView(dialogView)
                    .setCancelable(false)
                    .setPositiveButton("Continue", (dialog, which) -> {
                        // If the checkbox is not checked, set KEY_INITIAL_SETUP_DONE to true
                        if (!checkBox.isChecked()) {
                            PreferenceManager.getDefaultSharedPreferences(this)
                                    .edit()
                                    .putBoolean(KEY_INITIAL_SETUP_DONE, true)
                                    .apply();
                        }
                    })
                    .show();
        }
        
        if (ipList.isEmpty()) {
            ipList.add(new IpAddressRecord("Example", "8.8.8.8"));
        }

        adapter = new IpAddressAdapter(ipList, new IpAddressAdapter.OnPingClickListener() {
            @Override
            public void onPingClick(IpAddressRecord ip) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, TermuxConstants.PERMISSION_RUN_COMMAND) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Missing RUN_COMMAND permission!!", Toast.LENGTH_SHORT).show();
                    requestPermissions(new String[]{TermuxConstants.PERMISSION_RUN_COMMAND}, 13);
                    return;
                }
                String ipAddress = ip.getAddress();
                // Check if Termux is installed
                PackageManager pm = getPackageManager();
                Intent launchIntent = pm.getLaunchIntentForPackage("com.termux");
                if (launchIntent == null) {
                    Toast.makeText(MainActivity.this, "Termux is not installed.", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent();
                intent.setAction(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND);
                intent.setClassName(TermuxConstants.TERMUX_PACKAGE_NAME, TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE_NAME);
                intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_COMMAND_PATH, "/data/data/com.termux/files/usr/bin/ping");
                intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_ARGUMENTS, new String[]{"-c", "5", ipAddress});
                intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_BACKGROUND, false); // Show in Termux UI
                intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_ACTION, "com.termux.RUN_COMMAND_RESULT");
                startService(intent);
            }
            @Override
            public void onDeleteClick(int position, IpAddressRecord ip) {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Delete entry")
                        .setMessage("Are you sure you want to delete '" + ip.getLabel() + "' (" + ip.getAddress() + ")?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            adapter.removeIpAddress(position);
                            Toast.makeText(MainActivity.this, "Entry deleted", Toast.LENGTH_SHORT).show();
                            saveIpList();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
            @Override
            public void onLogClick(IpAddressRecord ip) {
                Toast.makeText(MainActivity.this, "Under development", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_ip, null);
            TextInputLayout inputLayoutLabel = dialogView.findViewById(R.id.inputLayoutLabel);
            TextInputLayout inputLayoutAddress = dialogView.findViewById(R.id.inputLayoutAddress);
            TextInputEditText editTextLabel = dialogView.findViewById(R.id.editTextLabel);
            TextInputEditText editTextAddress = dialogView.findViewById(R.id.editTextAddress);

            new MaterialAlertDialogBuilder(this)
                    .setTitle(" ")
                    .setView(dialogView)
                    .setCancelable(false)
                    .setPositiveButton("Add", (dialog, which) -> {
                        String label = editTextLabel.getText() != null ? editTextLabel.getText().toString().trim() : "";
                        String address = editTextAddress.getText() != null ? editTextAddress.getText().toString().trim() : "";
                        boolean valid = true;
                        if (label.isEmpty()) {
                            inputLayoutLabel.setError("Label required");
                            valid = false;
                        } else {
                            inputLayoutLabel.setError(null);
                        }
                        if (address.isEmpty()) {
                            inputLayoutAddress.setError("IP required");
                            valid = false;
                        } else {
                            inputLayoutAddress.setError(null);
                        }
                        if (!valid) {
                            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        IpAddressRecord newIp = new IpAddressRecord(label, address);
                        adapter.addIpAddress(newIp);
                        saveIpList();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void saveIpList() {
        String json = com.lsgio.termuxpinger.models.IpAddressRecord.toJsonList(ipList);
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(KEY_IP_LIST_JSON, json)
                .apply();
    }
}