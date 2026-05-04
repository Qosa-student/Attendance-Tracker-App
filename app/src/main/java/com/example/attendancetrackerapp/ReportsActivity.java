package com.example.attendancetrackerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportsActivity extends AppCompatActivity {

    LinearLayout llReportList, llFlagged;
    LinearLayout navHome, navAttendance, navScanner, navReports, navProfile;
    int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        llReportList = findViewById(R.id.llReportList);
        llFlagged = findViewById(R.id.llFlagged);

        navHome = findViewById(R.id.navHome);
        navAttendance = findViewById(R.id.navAttendance);
        navScanner = findViewById(R.id.navScanner);
        navReports = findViewById(R.id.navReports);
        navProfile = findViewById(R.id.navProfile);

        loadReportsOnline();
        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReportsOnline();
    }

    private void loadReportsOnline() {
        ApiService.create().getReports(userId).enqueue(new Callback<List<ApiService.ReportModel>>() {
            @Override
            public void onResponse(Call<List<ApiService.ReportModel>> call, Response<List<ApiService.ReportModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    renderReportsUI(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<ApiService.ReportModel>> call, Throwable t) {
                Toast.makeText(ReportsActivity.this, "Failed to load reports", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderReportsUI(List<ApiService.ReportModel> reports) {
        llReportList.removeAllViews();
        llFlagged.removeAllViews();
        boolean anyFlagged = false;

        for (ApiService.ReportModel r : reports) {
            if (r.total_count == 0) continue;

            int rate = Math.round((r.present_count * 100f) / r.total_count);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 0, 0, 12);

            TextView tvName = new TextView(this);
            tvName.setText(r.name + " · " + r.student_number + " (" + rate + "%)");
            tvName.setTextSize(13f);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvName.setTextColor(rate >= 75 ? 0xFF000000 : 0xFFCC0000);

            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            pb.setProgress(rate);
            pb.setLayoutParams(new LinearLayout.LayoutParams(-1, 20));
            pb.setProgressTintList(android.content.res.ColorStateList.valueOf(rate >= 75 ? 0xFF16A34A : 0xFFCC0000));

            row.addView(tvName);
            row.addView(pb);
            llReportList.addView(row);

            if (rate < 75) {
                anyFlagged = true;
                LinearLayout flagCard = new LinearLayout(this);
                flagCard.setOrientation(LinearLayout.VERTICAL);
                flagCard.setBackgroundColor(0xFFFEF2F2);
                flagCard.setPadding(24, 16, 24, 16);
                
                TextView tvFlagName = new TextView(this);
                tvFlagName.setText(r.name + " — AT RISK");
                tvFlagName.setTextColor(0xFF991B1B);
                tvFlagName.setTypeface(null, android.graphics.Typeface.BOLD);

                TextView tvFlagInfo = new TextView(this);
                tvFlagInfo.setText("Rate: " + rate + "% (" + r.present_count + "/" + r.total_count + " days)");
                tvFlagInfo.setTextColor(0xFFB91C1C);

                flagCard.addView(tvFlagName);
                flagCard.addView(tvFlagInfo);
                llFlagged.addView(flagCard);
            }
        }

        if (!anyFlagged) {
            TextView tvNone = new TextView(this);
            tvNone.setText("No flagged students.");
            tvNone.setTextColor(0xFF16A34A);
            llFlagged.addView(tvNone);
        }
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        navAttendance.setOnClickListener(v -> startActivity(new Intent(this, AttendanceActivity.class)));
        navScanner.setOnClickListener(v -> startActivity(new Intent(this, ScannerActivity.class)));
        navReports.setOnClickListener(v -> loadReportsOnline());
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }
}
