package com.smartreply.beta;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.net.Uri;
import android.content.Intent;
import android.view.View;
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
    private Spinner delaySpinner;
    private Spinner repeatSpinner;
    private Spinner simSpinner;
    private Switch chatbotSwitch;
    private EditText chatbotRulesInput;
    private EditText fallbackInput;
    private TextView statusText;
    private TextView permissionStatusText;
    private EditText testNumberInput;
    private final List<Integer> subscriptionIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enabledSwitch = findViewById(R.id.enabledSwitch);
        messageInput = findViewById(R.id.messageInput);
        delaySpinner = findViewById(R.id.delaySpinner);
        repeatSpinner = findViewById(R.id.repeatSpinner);
        simSpinner = findViewById(R.id.simSpinner);
        chatbotSwitch = findViewById(R.id.chatbotSwitch);
        chatbotRulesInput = findViewById(R.id.chatbotRulesInput);
        fallbackInput = findViewById(R.id.fallbackInput);
        statusText = findViewById(R.id.statusText);
        permissionStatusText = findViewById(R.id.permissionStatusText);
        testNumberInput = findViewById(R.id.testNumberInput);
        Button saveButton = findViewById(R.id.saveButton);
        Button permissionButton = findViewById(R.id.permissionButton);
        Button testSmsButton = findViewById(R.id.testSmsButton);
        Button loadMenuTemplateButton = findViewById(R.id.loadMenuTemplateButton);

        ArrayAdapter<CharSequence> delayAdapter = ArrayAdapter.createFromResource(
                this, R.array.delay_labels, android.R.layout.simple_spinner_item);
        delayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        delaySpinner.setAdapter(delayAdapter);

        ArrayAdapter<CharSequence> repeatAdapter = ArrayAdapter.createFromResource(
                this, R.array.repeat_labels, android.R.layout.simple_spinner_item);
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatSpinner.setAdapter(repeatAdapter);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.contains("last_processed_call")) {
            prefs.edit().putLong("last_processed_call", System.currentTimeMillis()).apply();
        }

        enabledSwitch.setChecked(prefs.getBoolean("enabled", false));
        messageInput.setText(prefs.getString("message", getString(R.string.default_message)));
        delaySpinner.setSelection(delayToIndex(prefs.getInt("delay_seconds", 30)));
        repeatSpinner.setSelection(repeatToIndex(prefs.getInt("repeat_minutes", 0)));
        chatbotSwitch.setChecked(prefs.getBoolean("chatbot_enabled", false));
        chatbotRulesInput.setText(prefs.getString("chatbot_rules", getString(R.string.default_chatbot_rules)));
        fallbackInput.setText(prefs.getString("chatbot_fallback", getString(R.string.default_chatbot_fallback)));

        saveButton.setOnClickListener(v -> saveSettings());
        permissionButton.setOnClickListener(v -> handlePermissionButton());
        testSmsButton.setOnClickListener(v -> sendTestSms());
        loadMenuTemplateButton.setOnClickListener(v -> loadMenuTemplate());
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean("enabled", isChecked)
                    .apply();
            updateStatus();
        });
        chatbotSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putBoolean("chatbot_enabled", isChecked)
                        .apply());

        // Do not request restricted SMS/Call Log permissions on launch.
        // The device owner must first choose to configure ReplyDesk and see the disclosure.
        loadSimCards();
        updateStatus();
        updatePermissionStatus();
    }

    private void loadMenuTemplate() {
        new AlertDialog.Builder(this)
                .setTitle("Load 1–6 menu template?")
                .setMessage("This replaces the rules and welcome menu on this screen. Copy any custom replies you want to keep first. Fill in your business details and notice periods, then save.")
                .setNegativeButton("Keep current replies", null)
                .setPositiveButton("Load template", (dialog, which) -> applyMenuTemplate())
                .show();
    }

    private void applyMenuTemplate() {
        chatbotRulesInput.setText(getString(R.string.menu_chatbot_rules));
        fallbackInput.setText(getString(R.string.menu_chatbot_fallback));
        chatbotSwitch.setChecked(true);
        Toast.makeText(this, "Menu template loaded. Review it, then tap Save settings.", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        updatePermissionStatus();
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
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.RECEIVE_SMS);
        }
        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[0]), PERMISSION_REQUEST);
        }
    }

    private void handlePermissionButton() {
        if (hasAllPermissions()) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            showRestrictedPermissionDisclosure();
        }
    }

    private void showRestrictedPermissionDisclosure() {
        new AlertDialog.Builder(this)
                .setTitle("Business SMS automation permissions")
                .setMessage("ReplyDesk is a user-configured business SMS tool. You decide whether automation is enabled, the message content, response rules, timing and SIM.\n\nTo run the rules you choose on this device, ReplyDesk needs Phone and Call log access to detect missed-call triggers, Send SMS access to send your configured response, and Receive SMS access to detect incoming-message triggers.\n\nReplyDesk does not choose your message content or enable automation for you. You can turn automation off at any time.")
                .setNegativeButton("Not now", null)
                .setPositiveButton("Continue", (dialog, which) -> requestRequiredPermissions())
                .show();
    }

    private boolean hasAllPermissions() {
        return checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void updatePermissionStatus() {
        StringBuilder text = new StringBuilder("Required permissions\n");
        appendPermission(text, "Phone", Manifest.permission.READ_PHONE_STATE);
        appendPermission(text, "Call log", Manifest.permission.READ_CALL_LOG);
        appendPermission(text, "Send SMS", Manifest.permission.SEND_SMS);
        appendPermission(text, "Receive SMS", Manifest.permission.RECEIVE_SMS);
        if (!hasAllPermissions()) {
            text.append("\nIf Samsung blocks SMS: App info → three-dot menu → Allow restricted settings, then return here.");
        }
        permissionStatusText.setText(text.toString());
    }

    private void appendPermission(StringBuilder text, String label, String permission) {
        boolean granted = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        text.append(granted ? "✓ " : "✕ ").append(label).append(granted ? ": allowed\n" : ": not allowed\n");
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

    private boolean saveSettings() {
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) {
            messageInput.setError("Please enter an auto-reply message");
            return false;
        }

        if (chatbotSwitch.isChecked()) {
            String rules = chatbotRulesInput.getText().toString();
            String welcome = fallbackInput.getText().toString();
            if (rules.toLowerCase(java.util.Locale.ROOT).contains("[enter ")
                    || welcome.toLowerCase(java.util.Locale.ROOT).contains("[enter ")) {
                chatbotRulesInput.setError("Fill in the business details and notice periods in brackets before enabling the chatbot");
                return false;
            }
        }
        int delay = indexToDelay(delaySpinner.getSelectedItemPosition());
        int repeatMinutes = indexToRepeat(repeatSpinner.getSelectedItemPosition());
        int simIndex = Math.max(0, simSpinner.getSelectedItemPosition());
        int subscriptionId = subscriptionIds.get(Math.min(simIndex, subscriptionIds.size() - 1));

        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean("enabled", enabledSwitch.isChecked())
                .putString("message", message)
                .putInt("delay_seconds", delay)
                .putInt("repeat_minutes", repeatMinutes)
                .putInt("subscription_id", subscriptionId)
                .putBoolean("chatbot_enabled", chatbotSwitch.isChecked())
                .putString("chatbot_rules", chatbotRulesInput.getText().toString().trim())
                .putString("chatbot_fallback", fallbackInput.getText().toString().trim())
                .apply();

        updateStatus();
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        return true;
    }

    private void sendTestSms() {
        if (!hasAllPermissions()) {
            updatePermissionStatus();
            Toast.makeText(this, "Allow all four permissions before testing", Toast.LENGTH_LONG).show();
            showRestrictedPermissionDisclosure();
            return;
        }
        String number = testNumberInput.getText().toString().trim();
        if (number.isEmpty()) {
            testNumberInput.setError("Enter another phone number, including country code");
            return;
        }
        if (!saveSettings()) return;
        try {
            SmsSender.send(this, number, messageInput.getText().toString().trim(), "manual_test");
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString("last_sms_status", "Sending test SMS…")
                    .putString("last_error", "")
                    .apply();
            updateStatus();
            Toast.makeText(this, "Test SMS requested. Check status below in a few seconds.", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString("last_error", error.getClass().getSimpleName() + ": " + error.getMessage())
                    .putString("last_sms_status", "Test failed before sending")
                    .apply();
            updateStatus();
            Toast.makeText(this, "Test failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateStatus() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean enabled = enabledSwitch.isChecked();
        long lastSent = prefs.getLong("last_sent_at", 0L);
        String lastNumber = prefs.getString("last_sent_number", "");
        String smsStatus = prefs.getString("last_sms_status", "No SMS test recorded yet");
        String lastError = prefs.getString("last_error", "");

        StringBuilder text = new StringBuilder(enabled ? "Auto reply is ON" : "Auto reply is OFF");
        if (lastSent > 0) {
            text.append("\nLast reply: ")
                    .append(maskNumber(lastNumber))
                    .append(" — ")
                    .append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(new Date(lastSent)));
        }
        text.append("\nSMS status: ").append(smsStatus);
        text.append("\nChatbot: ").append(prefs.getString("last_chat_status", "Waiting for an incoming SMS"));
        if (!lastError.isEmpty()) text.append("\nError: ").append(lastError);
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

    private int repeatToIndex(int minutes) {
        int[] values = {0, 15, 60, 360, 720, 1440};
        for (int i = 0; i < values.length; i++) if (values[i] == minutes) return i;
        return 0;
    }

    private int indexToRepeat(int index) {
        int[] values = {0, 15, 60, 360, 720, 1440};
        return values[Math.max(0, Math.min(index, values.length - 1))];
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
            updatePermissionStatus();
        }
    }
}
