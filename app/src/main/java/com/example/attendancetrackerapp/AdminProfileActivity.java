package com.example.attendancetrackerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AdminProfileActivity extends AppCompatActivity {

    TextView tvAdminInitials, tvAdminName, tvAdminInfoName, tvAdminInfoEmail;
    Button btnAdminLogout;
    LinearLayout navDashboard, navAdminProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        tvAdminInitials = findViewById(R.id.tvAdminInitials);
        tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminInfoName = findViewById(R.id.tvAdminInfoName);
        tvAdminInfoEmail = findViewById(R.id.tvAdminInfoEmail);
        btnAdminLogout = findViewById(R.id.btnAdminLogout);
        navDashboard = findViewById(R.id.navDashboard);
        navAdminProfile = findViewById(R.id.navAdminProfile);

        SharedPreferences prefs = getSharedPreferences(
                "user_session", MODE_PRIVATE);
        String name = prefs.getString("name", "Admin");
        String email = prefs.getString("email", "");

        String initials = "";
        for (String part : name.split(" ")) {
            if (!part.isEmpty()) initials += part.charAt(0);
            if (initials.length() == 2) break;
        }

        tvAdminInitials.setText(initials.toUpperCase());
        tvAdminName.setText(name);
        tvAdminInfoName.setText(name);
        tvAdminInfoEmail.setText(email);

        btnAdminLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        navDashboard.setOnClickListener(v ->
                startActivity(new Intent(this,
                        AdminDashboardActivity.class)));
    }
}