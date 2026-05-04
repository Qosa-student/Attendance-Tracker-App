package com.example.attendancetrackerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    TextView tvWelcome, tvDate, tvPresentCount,
            tvAbsentCount, tvLateCount, tvRateCount;
    LinearLayout llAtRisk;
    LinearLayout navHome, navAttendance, navScanner, navReports, navProfile;
    String todayDb;
    int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SharedPreferences prefs = getSharedPreferences(
                "user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        todayDb = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()).format(new Date());

        String todayDisplay = new SimpleDateFormat(
                "EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date());

        tvWelcome = findViewById(R.id.tvWelcome);
        tvDate = findViewById(R.id.tvDate);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        tvLateCount = findViewById(R.id.tvLateCount);
        tvRateCount = findViewById(R.id.tvRateCount);
        llAtRisk = findViewById(R.id.llAtRisk);

        navHome = findViewById(R.id.navHome);
        navAttendance = findViewById(R.id.navAttendance);
        navScanner = findViewById(R.id.navScanner);
        navReports = findViewById(R.id.navReports);
        navProfile = findViewById(R.id.navProfile);

        tvDate.setText(todayDisplay);

        loadDashboardData();
        setupNavigation();

        String userName = prefs.getString("name", "Teacher");
        tvWelcome.setText(String.format("Good day, %s!", userName));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {
        ApiService.create().getSummary(userId, todayDb).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiService.SummaryModel> call, Response<ApiService.SummaryModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.SummaryModel summary = response.body();
                    
                    tvPresentCount.setText(String.valueOf(summary.present));
                    tvAbsentCount.setText(String.valueOf(summary.absent));
                    tvLateCount.setText(String.valueOf(summary.late));
                    
                    int total = summary.present + summary.absent + summary.late;
                    int rate = total > 0 ? Math.round((summary.present * 100f) / total) : 0;
                    tvRateCount.setText(String.format(Locale.getDefault(), "%d%%", rate));

                    updateAtRiskUI(summary.at_risk);
                }
            }

            @Override
            public void onFailure(Call<ApiService.SummaryModel> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Failed to load dashboard data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAtRiskUI(java.util.List<ApiService.AtRiskModel> atRiskList) {
        llAtRisk.removeAllViews();
        
        if (atRiskList == null || atRiskList.isEmpty()) {
            TextView tvNone = new TextView(this);
            tvNone.setText("No at-risk students right now.");
            tvNone.setTextSize(13f);
            tvNone.setTextColor(0xFF555555);
            llAtRisk.addView(tvNone);
            return;
        }

        for (ApiService.AtRiskModel student : atRiskList) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(0xFFFEF2F2);
            card.setPadding(24, 16, 24, 16);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 8);
            card.setLayoutParams(params);

            TextView tvName = new TextView(this);
            tvName.setText(String.format("%s — %s", student.name, student.student_number));
            tvName.setTextSize(14f);
            tvName.setTextColor(0xFF991B1B);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvRate = new TextView(this);
            tvRate.setText(String.format(Locale.getDefault(), "Attendance rate: %d%% — below 75%% threshold", student.rate));
            tvRate.setTextSize(12f);
            tvRate.setTextColor(0xFFB91C1C);

            card.addView(tvName);
            card.addView(tvRate);
            llAtRisk.addView(card);
        }
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> loadDashboardData());
        navAttendance.setOnClickListener(v -> startActivity(new Intent(this, AttendanceActivity.class)));
        navScanner.setOnClickListener(v -> startActivity(new Intent(this, ScannerActivity.class)));
        navReports.setOnClickListener(v -> startActivity(new Intent(this, ReportsActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }
}
