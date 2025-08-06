package com.lsgio.termuxpinger;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lsgio.termuxpinger.adapters.AddressAdapter;
import com.lsgio.termuxpinger.models.AddressRecord;
import com.lsgio.termuxpinger.utils.TermuxConstants;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private AddressAdapter adapter;
    private ArrayList<AddressRecord> addressList;
    private static final String KEY_INITIAL_SETUP_DONE = "isInitialSetupDone";
    private static final String KEY_IP_LIST_JSON = "ipListJson";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load the list from SharedPreferences
        String json = PreferenceManager.getDefaultSharedPreferences(this).getString(KEY_IP_LIST_JSON, null);
        if (json != null) {
            addressList = new ArrayList<>(AddressRecord.fromJsonString(json));
        } else {
            addressList = new ArrayList<>();
        }
        // Add an example entry if you want it to appear on first launch
        boolean isInitialSetupDone = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KEY_INITIAL_SETUP_DONE, true);
        if (isInitialSetupDone) {
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
        
        if (addressList.isEmpty()) {
            addressList.add(new AddressRecord("Example", "8.8.8.8"));
        }

        adapter = new AddressAdapter(addressList, new AddressAdapter.OnPingClickListener() {
            @Override
            public void onPingClick(AddressRecord address) {
                String ipAddress = address.getAddress();
                // Check if Termux is installed
                PackageManager pm = getPackageManager();
                try {
                    pm.getApplicationInfo(TermuxConstants.TERMUX_PACKAGE_NAME, 0);
                } catch (Exception e) {
                    toastException(e);
                    return;
                }
                if (ContextCompat.checkSelfPermission(MainActivity.this, TermuxConstants.PERMISSION_RUN_COMMAND) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Missing RUN_COMMAND permission!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent();
                intent.setAction(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND);
                intent.setClassName(TermuxConstants.TERMUX_PACKAGE_NAME, TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE_NAME);
                intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_COMMAND_PATH, "/data/data/com.termux/files/usr/bin/ping");
                intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_ARGUMENTS, new String[]{ipAddress});
                intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_BACKGROUND, false); // Show in Termux UI
                intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_ACTION, "com.termux.RUN_COMMAND_RESULT");
                try {
                    startService(intent);
                } catch (Exception e) {
                    toastException(e);
                    return;
                }
            }
            @Override
            public void onDeleteClick(int position, AddressRecord address) {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Delete entry")
                        .setMessage("Are you sure you want to delete '" + address.getLabel() + "' (" + address.getAddress() + ")?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            adapter.removeIpAddress(position);
                            Toast.makeText(MainActivity.this, "Entry deleted", Toast.LENGTH_SHORT).show();
                            saveIpList();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
            @Override
            public void onLogClick(AddressRecord address) {
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
                        AddressRecord newIp = new AddressRecord(label, address);
                        adapter.addIpAddress(newIp);
                        saveIpList();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void toastException(Exception e) {
        if (e == null) {
            return;
        }
        Toast.makeText(MainActivity.this, "Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        return;
    }

    private void saveIpList() {
        String json = AddressRecord.toJsonList(addressList);
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(KEY_IP_LIST_JSON, json)
                .apply();
    }
}