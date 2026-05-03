package com.example.attendancetrackerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ReportsActivity extends AppCompatActivity {

    LinearLayout llReportList, llFlagged;
    LinearLayout navHome, navAttendance, navScanner, navReports, navProfile;
    DataBase db;
    int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        db = new DataBase(this);
        
        SharedPreferences prefs = getSharedPreferences(
                "user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        llReportList = findViewById(R.id.llReportList);
        llFlagged = findViewById(R.id.llFlagged);

        navHome = findViewById(R.id.navHome);
        navAttendance = findViewById(R.id.navAttendance);
        navScanner = findViewById(R.id.navScanner);
        navReports = findViewById(R.id.navReports);
        navProfile = findViewById(R.id.navProfile);

        loadReports();
        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReports();
    }

    private void loadReports() {
        llReportList.removeAllViews();
        llFlagged.removeAllViews();

        Cursor cursor = db.getAttendanceSummary(userId);
        boolean anyFlagged = false;

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name"));
                String studentNumber = cursor.getString(
                        cursor.getColumnIndexOrThrow("student_number"));
                int presentCount = cursor.getInt(
                        cursor.getColumnIndexOrThrow("present_count"));
                int totalCount = cursor.getInt(
                        cursor.getColumnIndexOrThrow("total_count"));

                if (totalCount == 0) continue;

                int rate = Math.round((presentCount * 100f) / totalCount);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(0, 0, 0, 12);

                LinearLayout nameRow = new LinearLayout(this);
                nameRow.setOrientation(LinearLayout.HORIZONTAL);
                nameRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

                TextView tvName = new TextView(this);
                tvName.setText(name + " · " + studentNumber);
                tvName.setTextSize(13f);
                tvName.setTextColor(0xFF000000);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                LinearLayout.LayoutParams nameParams =
                        new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tvName.setLayoutParams(nameParams);

                TextView tvRate = new TextView(this);
                tvRate.setText(rate + "%");
                tvRate.setTextSize(13f);
                tvRate.setTypeface(null, android.graphics.Typeface.BOLD);
                if (rate >= 80) tvRate.setTextColor(0xFF16A34A);
                else if (rate >= 75) tvRate.setTextColor(0xFF1A3A8F);
                else tvRate.setTextColor(0xFFCC0000);

                nameRow.addView(tvName);
                nameRow.addView(tvRate);

                ProgressBar progressBar = new ProgressBar(
                        this, null,
                        android.R.attr.progressBarStyleHorizontal);
                progressBar.setMax(100);
                progressBar.setProgress(rate);
                LinearLayout.LayoutParams pbParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 20);
                pbParams.setMargins(0, 4, 0, 0);
                progressBar.setLayoutParams(pbParams);

                if (rate >= 80) {
                    progressBar.setProgressTintList(
                            android.content.res.ColorStateList.valueOf(0xFF16A34A));
                } else if (rate >= 75) {
                    progressBar.setProgressTintList(
                            android.content.res.ColorStateList.valueOf(0xFF1A3A8F));
                } else {
                    progressBar.setProgressTintList(
                            android.content.res.ColorStateList.valueOf(0xFFCC0000));
                }

                row.addView(nameRow);
                row.addView(progressBar);
                llReportList.addView(row);

                if (rate < 75) {
                    anyFlagged = true;
                    LinearLayout flagCard = new LinearLayout(this);
                    flagCard.setOrientation(LinearLayout.VERTICAL);
                    flagCard.setBackgroundColor(0xFFFEF2F2);
                    flagCard.setPadding(24, 16, 24, 16);
                    LinearLayout.LayoutParams flagParams =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    flagParams.setMargins(0, 0, 0, 8);
                    flagCard.setLayoutParams(flagParams);

                    TextView tvFlagName = new TextView(this);
                    tvFlagName.setText(name + " — " + studentNumber);
                    tvFlagName.setTextSize(13f);
                    tvFlagName.setTextColor(0xFF991B1B);
                    tvFlagName.setTypeface(null, android.graphics.Typeface.BOLD);

                    TextView tvFlagRate = new TextView(this);
                    tvFlagRate.setText("Rate: " + rate +
                            "% — below 75% threshold. Present: " +
                            presentCount + "/" + totalCount + " days");
                    tvFlagRate.setTextSize(12f);
                    tvFlagRate.setTextColor(0xFFB91C1C);

                    flagCard.addView(tvFlagName);
                    flagCard.addView(tvFlagRate);
                    llFlagged.addView(flagCard);
                }

            } while (cursor.moveToNext());
        }
        cursor.close();

        if (!anyFlagged) {
            TextView tvNone = new TextView(this);
            tvNone.setText("No flagged students. All above 75%.");
            tvNone.setTextSize(13f);
            tvNone.setTextColor(0xFF16A34A);
            llFlagged.addView(tvNone);
        }
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
        navAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(this, AttendanceActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
        navScanner.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScannerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
        navReports.setOnClickListener(v -> {
            // already on reports, just refresh
            loadReports();
        });
        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
    }
}