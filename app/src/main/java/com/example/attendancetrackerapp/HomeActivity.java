package com.example.attendancetrackerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    TextView tvWelcome, tvDate, tvPresentCount,
            tvAbsentCount, tvLateCount, tvRateCount;
    LinearLayout llAtRisk;
    LinearLayout navHome, navAttendance, navScanner, navReports, navProfile;
    DataBase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = new DataBase(this);

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

        String today = new SimpleDateFormat(
                "EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date());
        tvDate.setText(today);

        loadSummary();
        loadAtRisk();
        setupNavigation();

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        Button btnClockIn = findViewById(R.id.btnClockIn);
        Button btnClockOut = findViewById(R.id.btnClockOut);

        btnClockIn.setOnClickListener(v -> {
            String time = new SimpleDateFormat(
                    "HH:mm", Locale.getDefault()).format(new Date());

            Cursor record = db.getTeacherRecordForToday(userId, today);
            String expectedStart = "08:00";

            Cursor userCursor = db.getUserById(userId);
            if (userCursor.moveToFirst()) {
                expectedStart = userCursor.getString(
                        userCursor.getColumnIndexOrThrow("expected_start"));
            }
            userCursor.close();

            boolean success = db.clockIn(userId, today, time);
            if (success) {
                boolean isLate = time.compareTo(expectedStart) > 0;
                if (isLate) {
                    db.adminMarkTeacher(userId, today, "late");
                    Toast.makeText(this,
                            "Clocked in at " + time + " (Late)",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this,
                            "Clocked in at " + time,
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this,
                        "Already clocked in today.",
                        Toast.LENGTH_SHORT).show();
            }
            record.close();
        });

        btnClockOut.setOnClickListener(v -> {
            String time = new SimpleDateFormat(
                    "HH:mm", Locale.getDefault()).format(new Date());
            boolean success = db.clockOut(userId, today, time);
            if (success) {
                Toast.makeText(this,
                        "Clocked out at " + time,
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "Please clock in first.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSummary();
        loadAtRisk();
    }

    private void loadSummary() {
        String today = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Cursor cursor = db.getAttendanceByDate(today);

        int present = 0, absent = 0, late = 0;

        if (cursor.moveToFirst()) {
            do {
                String status = cursor.getString(
                        cursor.getColumnIndexOrThrow("status"));
                if (status.equals("present")) present++;
                else if (status.equals("absent")) absent++;
                else if (status.equals("late")) late++;
            } while (cursor.moveToNext());
        }
        cursor.close();

        int total = present + absent + late;
        int rate = total > 0 ? Math.round((present * 100f) / total) : 0;

        tvPresentCount.setText(String.valueOf(present));
        tvAbsentCount.setText(String.valueOf(absent));
        tvLateCount.setText(String.valueOf(late));
        tvRateCount.setText(rate + "%");
    }

    private void loadAtRisk() {
        llAtRisk.removeAllViews();

        Cursor cursor = db.getAttendanceSummary();

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

                if (rate < 75) {
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
                    tvName.setText(name + " — " + studentNumber);
                    tvName.setTextSize(14f);
                    tvName.setTextColor(0xFF991B1B);
                    tvName.setTypeface(null,
                            android.graphics.Typeface.BOLD);

                    TextView tvRate = new TextView(this);
                    tvRate.setText("Attendance rate: " + rate +
                            "% — below 75% threshold");
                    tvRate.setTextSize(12f);
                    tvRate.setTextColor(0xFFB91C1C);

                    card.addView(tvName);
                    card.addView(tvRate);
                    llAtRisk.addView(card);
                }

            } while (cursor.moveToNext());
        }
        cursor.close();

        if (llAtRisk.getChildCount() == 0) {
            TextView tvNone = new TextView(this);
            tvNone.setText("No at-risk students right now.");
            tvNone.setTextSize(13f);
            tvNone.setTextColor(0xFF555555);
            llAtRisk.addView(tvNone);
        }
    }

    private void setupNavigation() {
        navAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(
                        HomeActivity.this, AttendanceActivity.class));
            }
        });

        navScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(
                        HomeActivity.this, ScannerActivity.class));
            }
        });

        navReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(
                        HomeActivity.this, ReportsActivity.class));
            }
        });

        navProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(
                        HomeActivity.this, ProfileActivity.class));
            }
        });
    }
}