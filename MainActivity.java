package com.smartreply.beta;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST = 1001;
    private static final String PREFS = "smart_reply_settings";

    private Switch enabledSwitch;
    private EditText messageInput;
    private EditText licenseInput;
    private Spinner delaySpinner;
    private Spinner simSpinner;
    private TextView statusText;
    private TextView licenseStatusText;
    private Button saveButton;
    private final List<Integer> subscriptionIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enabledSwitch = findViewById(R.id.enabledSwitch);
        messageInput = findViewById(R.id.messageInput);
        licenseInput = findViewById(R.id.licenseInput);
        delaySpinner = findViewById(R.id.delaySpinner);
        simSpinner = findViewById(R.id.simSpinner);
        statusText = findViewById(R.id.statusText);
        licenseStatusText = findViewById(R.id.licenseStatusText);
        saveButton = findViewById(R.id.saveButton);
        Button activateButton = findViewById(R.id.activateButton);

        ArrayAdapter<CharSequence> delayAdapter = ArrayAdapter.createFromResource(
                this, R.array.delay_labels, android.R.layout.simple_spinner_item);
        delayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        delaySpinner.setAdapter(delayAdapter);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.contains("last_processed_call")) {
            prefs.edit().putLong("last_processed_call", System.currentTimeMillis()).apply();
        }

        enabledSwitch.setChecked(prefs.getBoolean("enabled", false));
        messageInput.setText(prefs.getString("message", getString(R.string.default_message)));
        delaySpinner.setSelection(delayToIndex(prefs.getInt("delay_seconds", 30)));

        activateButton.setOnClickListener(v -> activateLicense());
        saveButton.setOnClickListener(v -> saveSettings());
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> updateStatus());

        refreshLicenseUi();
        if (LicenseManager.isActive(this)) {
            requestRequiredPermissions();
            loadSimCards();
        }
        updateStatus();
    }

    private void activateLicense() {
        LicenseManager.ActivationResult result =
                LicenseManager.activate(this, licenseInput.getText().toString());

        Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
        refreshLicenseUi();

        if (result.success) {
            licenseInput.setText("");
            requestRequiredPermissions();
            loadSimCards();
        }
    }

    private void refreshLicenseUi() {
        boolean active = LicenseManager.isActive(this);
        enabledSwitch.setEnabled(active);
        messageInput.setEnabled(active);
        delaySpinner.setEnabled(active);
        simSpinner.setEnabled(active);
        saveButton.setEnabled(active);

        if (!active) {
            enabledSwitch.setChecked(false);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("enabled", false).apply();
            licenseStatusText.setText("License required or expired");
        } else {
            licenseStatusText.setText("Free test license active — "
                    + LicenseManager.daysRemaining(this) + " days remaining");
        }
    }

    private void requestRequiredPermissions() {
        List<String> missing = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.READ_CALL_LOG);
        }
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.SEND_SMS);
        }
        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[0]), PERMISSION_REQUEST);
        }
    }

    private void loadSimCards() {
        List<String> labels = new ArrayList<>();
        subscriptionIds.clear();

        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            SubscriptionManager manager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            List<SubscriptionInfo> subscriptions = manager.getActiveSubscriptionInfoList();
            if (subscriptions != null) {
                for (SubscriptionInfo info : subscriptions) {
                    subscriptionIds.add(info.getSubscriptionId());
                    String carrier = String.valueOf(info.getCarrierName());
                    labels.add("SIM " + (info.getSimSlotIndex() + 1) + " — " + carrier);
                }
            }
        }

        if (labels.isEmpty()) {
            labels.add("Default SIM");
            subscriptionIds.add(SubscriptionManager.getDefaultSmsSubscriptionId());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        simSpinner.setAdapter(adapter);

        int savedId = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt("subscription_id", SubscriptionManager.getDefaultSmsSubscriptionId());
        int selectedIndex = subscriptionIds.indexOf(savedId);
        simSpinner.setSelection(selectedIndex >= 0 ? selectedIndex : 0);
    }

    private void saveSettings() {
        if (!LicenseManager.isActive(this)) {
            Toast.makeText(this, "Activate a valid license first", Toast.LENGTH_LONG).show();
            refreshLicenseUi();
            return;
        }

        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) {
            messageInput.setError("Please enter an auto-reply message");
            return;
        }

        int delay = indexToDelay(delaySpinner.getSelectedItemPosition());
        int simIndex = Math.max(0, simSpinner.getSelectedItemPosition());
        int subscriptionId = subscriptionIds.get(Math.min(simIndex, subscriptionIds.size() - 1));

        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean("enabled", enabledSwitch.isChecked())
                .putString("message", message)
                .putInt("delay_seconds", delay)
                .putInt("subscription_id", subscriptionId)
                .apply();

        updateStatus();
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private void updateStatus() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean active = LicenseManager.isActive(this);
        boolean enabled = active && enabledSwitch.isChecked();
        long lastSent = prefs.getLong("last_sent_at", 0L);
        String lastNumber = prefs.getString("last_sent_number", "");

        StringBuilder text = new StringBuilder();
        if (!active) {
            text.append("Auto reply is locked — activate a license");
        } else {
            text.append(enabled ? "Auto reply is ON" : "Auto reply is OFF");
        }

        if (lastSent > 0) {
            text.append("\nLast reply: ")
                    .append(maskNumber(lastNumber))
                    .append(" — ")
                    .append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(new Date(lastSent)));
        }
        statusText.setText(text.toString());
    }

    private String maskNumber(String number) {
        if (number == null || number.length() < 4) return "Unknown number";
        return "••••" + number.substring(number.length() - 4);
    }

    private int delayToIndex(int delay) {
        if (delay == 0) return 0;
        if (delay == 15) return 1;
        if (delay == 60) return 3;
        return 2;
    }

    private int indexToDelay(int index) {
        int[] delays = {0, 15, 30, 60};
        return delays[Math.max(0, Math.min(index, delays.length - 1))];
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            loadSimCards();
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "Phone, call log and SMS permissions are required", Toast.LENGTH_LONG).show();
            }
        }
    }
}
