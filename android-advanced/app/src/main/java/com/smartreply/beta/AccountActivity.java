package com.smartreply.beta;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class AccountActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        TextView plan = findViewById(R.id.planSummaryText);
        plan.setText("ReplyDesk Core\n14-day trial\nMonthly: A$30\nYearly: A$299");

        ((Button) findViewById(R.id.manageSubscriptionButton)).setOnClickListener(v ->
                Toast.makeText(this, "Store subscription management will be connected before release.", Toast.LENGTH_LONG).show());

        ((Button) findViewById(R.id.deleteAccountButton)).setOnClickListener(v ->
                Toast.makeText(this, "Account deletion will be completed through the secured ReplyDesk backend after sign-in is connected.", Toast.LENGTH_LONG).show());
    }
}
