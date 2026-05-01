package com.example.attendancetrackerapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    LinearLayout llTodayList, llMonthlyList, llHighlights, llScheduleList;
    View layoutToday, layoutMonthly, layoutSchedules;
    Button btnTabToday, btnTabMonthly, btnTabSchedules;
    LinearLayout navDashboard, navAdminProfile;
    TextView tvAdminDate;
    DataBase db;
    String today, yearMonth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = new DataBase(this);
        today = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()).format(new Date());
        yearMonth = new SimpleDateFormat(
                "yyyy-MM", Locale.getDefault()).format(new Date());

        tvAdminDate = findViewById(R.id.tvAdminName);
        tvAdminDate.setText(new SimpleDateFormat(
                "EEEE, MMMM d, yyyy",
                Locale.getDefault()).format(new Date()));

        llTodayList = findViewById(R.id.llTodayList);
        llMonthlyList = findViewById(R.id.llMonthlyList);
        llHighlights = findViewById(R.id.llHighlights);
        llScheduleList = findViewById(R.id.llScheduleList);
        layoutToday = findViewById(R.id.layoutToday);
        layoutMonthly = findViewById(R.id.layoutMonthly);
        layoutSchedules = findViewById(R.id.layoutSchedules);
        btnTabToday = findViewById(R.id.btnTabToday);
        btnTabMonthly = findViewById(R.id.btnTabMonthly);
        btnTabSchedules = findViewById(R.id.btnTabSchedules);
        navDashboard = findViewById(R.id.navDashboard);
        navAdminProfile = findViewById(R.id.navAdminProfile);

        loadTodayList();
        loadMonthlyStats();
        loadSchedules();

        btnTabToday.setOnClickListener(v -> switchTab(0));
        btnTabMonthly.setOnClickListener(v -> switchTab(1));
        btnTabSchedules.setOnClickListener(v -> switchTab(2));

        navAdminProfile.setOnClickListener(v ->
                startActivity(new Intent(this, AdminProfileActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodayList();
        loadMonthlyStats();
        loadSchedules();
    }

    private void loadTodayList() {
        llTodayList.removeAllViews();
        Cursor cursor = db.getTeacherAttendanceToday(today);

        if (cursor.moveToFirst()) {
            do {
                int teacherId = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name"));
                String expectedStart = cursor.getString(
                        cursor.getColumnIndexOrThrow("expected_start"));
                String expectedEnd = cursor.getString(
                        cursor.getColumnIndexOrThrow("expected_end"));
                String clockIn = cursor.getString(
                        cursor.getColumnIndexOrThrow("clock_in"));
                String clockOut = cursor.getString(
                        cursor.getColumnIndexOrThrow("clock_out"));
                String status = cursor.getString(
                        cursor.getColumnIndexOrThrow("status"));

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackgroundColor(0xFFF5F5F5);
                card.setPadding(24, 16, 24, 16);
                LinearLayout.LayoutParams cp =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                cp.setMargins(0, 0, 0, 10);
                card.setLayoutParams(cp);

                LinearLayout topRow = new LinearLayout(this);
                topRow.setOrientation(LinearLayout.HORIZONTAL);
                topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

                LinearLayout info = new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams ip =
                        new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                info.setLayoutParams(ip);

                TextView tvName = new TextView(this);
                tvName.setText(name);
                tvName.setTextSize(14f);
                tvName.setTextColor(0xFF000000);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);

                TextView tvSchedule = new TextView(this);
                tvSchedule.setText("Expected: " + expectedStart
                        + " – " + expectedEnd);
                tvSchedule.setTextSize(11f);
                tvSchedule.setTextColor(0xFF1A3A8F);

                TextView tvClockInfo = new TextView(this);
                String clockText = "Clock In: " +
                        (clockIn != null ? clockIn : "—") +
                        "   Clock Out: " +
                        (clockOut != null ? clockOut : "—");
                tvClockInfo.setText(clockText);
                tvClockInfo.setTextSize(11f);
                tvClockInfo.setTextColor(0xFF555555);

                info.addView(tvName);
                info.addView(tvSchedule);
                info.addView(tvClockInfo);

                TextView tvStatus = new TextView(this);
                String displayStatus = status != null ? status : "absent";
                tvStatus.setText(displayStatus.toUpperCase()
                        .replace("_", " "));
                tvStatus.setTextSize(10f);
                tvStatus.setPadding(10, 4, 10, 4);
                tvStatus.setTextColor(0xFFFFFFFF);
                switch (displayStatus) {
                    case "present":
                        tvStatus.setBackgroundColor(0xFF16A34A); break;
                    case "late":
                        tvStatus.setBackgroundColor(0xFFD97706); break;
                    case "sick_leave":
                        tvStatus.setBackgroundColor(0xFF1A3A8F); break;
                    case "personal_leave":
                        tvStatus.setBackgroundColor(0xFF7C3AED); break;
                    default:
                        tvStatus.setBackgroundColor(0xFFCC0000); break;
                }

                topRow.addView(info);
                topRow.addView(tvStatus);
                card.addView(topRow);

                if (status == null || status.equals("absent")) {
                    LinearLayout btnRow = new LinearLayout(this);
                    btnRow.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams brp =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    brp.setMargins(0, 8, 0, 0);
                    btnRow.setLayoutParams(brp);

                    String[] labels = {"Sick Leave",
                            "Personal Leave", "Unexcused"};
                    String[] values = {"sick_leave",
                            "personal_leave", "absent"};
                    int[] colors = {0xFF1A3A8F, 0xFF7C3AED, 0xFFCC0000};

                    for (int i = 0; i < labels.length; i++) {
                        final String val = values[i];
                        final String lbl = labels[i];
                        Button btn = new Button(this);
                        btn.setText(lbl);
                        btn.setTextSize(10f);
                        btn.setTextColor(0xFFFFFFFF);
                        LinearLayout.LayoutParams bp =
                                new LinearLayout.LayoutParams(
                                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        bp.setMargins(0, 0, i < 2 ? 4 : 0, 0);
                        btn.setLayoutParams(bp);
                        btn.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(
                                        colors[i]));
                        btn.setOnClickListener(v -> {
                            new AlertDialog.Builder(this)
                                    .setTitle("Mark as " + lbl)
                                    .setMessage("Mark " + name +
                                            " as " + lbl + " for today?")
                                    .setPositiveButton("Confirm", (d, w) -> {
                                        db.adminMarkTeacher(
                                                teacherId, today, val);
                                        Toast.makeText(this,
                                                name + " marked as " + lbl,
                                                Toast.LENGTH_SHORT).show();
                                        loadTodayList();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });
                        btnRow.addView(btn);
                    }
                    card.addView(btnRow);
                }

                llTodayList.addView(card);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void loadMonthlyStats() {
        llHighlights.removeAllViews();
        llMonthlyList.removeAllViews();

        Cursor cursor = db.getAllTeacherStats(yearMonth);

        String mostOnTime = null, mostLate = null;
        int highestOnTime = -1, highestLate = -1;

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name"));
                int onTime = cursor.getInt(
                        cursor.getColumnIndexOrThrow("on_time"));
                int lateCount = cursor.getInt(
                        cursor.getColumnIndexOrThrow("late_count"));
                int total = cursor.getInt(
                        cursor.getColumnIndexOrThrow("total"));

                if (onTime > highestOnTime) {
                    highestOnTime = onTime;
                    mostOnTime = name;
                }
                if (lateCount > highestLate) {
                    highestLate = lateCount;
                    mostLate = name;
                }

                int rate = total > 0 ?
                        Math.round((onTime * 100f) / total) : 0;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(0, 0, 0, 12);

                LinearLayout nameRow = new LinearLayout(this);
                nameRow.setOrientation(LinearLayout.HORIZONTAL);
                nameRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

                TextView tvName = new TextView(this);
                tvName.setText(name);
                tvName.setTextSize(13f);
                tvName.setTextColor(0xFF000000);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                LinearLayout.LayoutParams np =
                        new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tvName.setLayoutParams(np);

                TextView tvRate = new TextView(this);
                tvRate.setText(rate + "% on time");
                tvRate.setTextSize(12f);
                tvRate.setTextColor(rate >= 80 ? 0xFF16A34A :
                        rate >= 60 ? 0xFFD97706 : 0xFFCC0000);
                tvRate.setTypeface(null, android.graphics.Typeface.BOLD);

                nameRow.addView(tvName);
                nameRow.addView(tvRate);

                ProgressBar pb = new ProgressBar(this, null,
                        android.R.attr.progressBarStyleHorizontal);
                pb.setMax(100);
                pb.setProgress(rate);
                LinearLayout.LayoutParams pbp =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 20);
                pbp.setMargins(0, 4, 0, 2);
                pb.setLayoutParams(pbp);
                pb.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(
                                rate >= 80 ? 0xFF16A34A :
                                        rate >= 60 ? 0xFFD97706 : 0xFFCC0000));

                TextView tvDetail = new TextView(this);
                tvDetail.setText("Present: " + onTime +
                        " | Late: " + lateCount +
                        " | Total recorded: " + total);
                tvDetail.setTextSize(11f);
                tvDetail.setTextColor(0xFF888888);

                row.addView(nameRow);
                row.addView(pb);
                row.addView(tvDetail);

                View divider = new View(this);
                divider.setBackgroundColor(0xFFEEEEEE);
                LinearLayout.LayoutParams dp =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1);
                dp.setMargins(0, 8, 0, 0);
                divider.setLayoutParams(dp);
                row.addView(divider);

                llMonthlyList.addView(row);
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (mostOnTime != null) {
            addHighlightCard(llHighlights,
                    mostOnTime + " was most punctual this month",
                    0xFFDCFCE7, 0xFF15803D);
        }
        if (mostLate != null && highestLate > 0) {
            addHighlightCard(llHighlights,
                    mostLate + " had the most late arrivals this month",
                    0xFFFEF9C3, 0xFFA16207);
        }
    }

    private void addHighlightCard(LinearLayout parent,
                                  String text, int bgColor, int textColor) {
        LinearLayout card = new LinearLayout(this);
        card.setBackgroundColor(bgColor);
        card.setPadding(24, 14, 24, 14);
        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, 8);
        card.setLayoutParams(cp);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setTextColor(textColor);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tv);
        parent.addView(card);
    }

    private void loadSchedules() {
        llScheduleList.removeAllViews();
        Cursor cursor = db.getAllTeachers();

        if (cursor.moveToFirst()) {
            do {
                int teacherId = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name"));
                String expectedStart = cursor.getString(
                        cursor.getColumnIndexOrThrow("expected_start"));
                String expectedEnd = cursor.getString(
                        cursor.getColumnIndexOrThrow("expected_end"));

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackgroundColor(0xFFF5F5F5);
                card.setPadding(24, 16, 24, 16);
                LinearLayout.LayoutParams cp =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                cp.setMargins(0, 0, 0, 10);
                card.setLayoutParams(cp);

                TextView tvName = new TextView(this);
                tvName.setText(name);
                tvName.setTextSize(14f);
                tvName.setTextColor(0xFF000000);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);

                TextView tvSched = new TextView(this);
                tvSched.setText("Current schedule: " +
                        expectedStart + " – " + expectedEnd);
                tvSched.setTextSize(12f);
                tvSched.setTextColor(0xFF555555);
                LinearLayout.LayoutParams tvp =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                tvp.setMargins(0, 4, 0, 8);
                tvSched.setLayoutParams(tvp);

                LinearLayout timeRow = new LinearLayout(this);
                timeRow.setOrientation(LinearLayout.HORIZONTAL);
                timeRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

                android.widget.TimePicker tpStart =
                        new android.widget.TimePicker(this);
                tpStart.setIs24HourView(true);
                tpStart.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                android.widget.TimePicker tpEnd =
                        new android.widget.TimePicker(this);
                tpEnd.setIs24HourView(true);
                tpEnd.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                try {
                    String[] startParts = expectedStart.split(":");
                    String[] endParts = expectedEnd.split(":");
                    tpStart.setHour(Integer.parseInt(startParts[0]));
                    tpStart.setMinute(Integer.parseInt(startParts[1]));
                    tpEnd.setHour(Integer.parseInt(endParts[0]));
                    tpEnd.setMinute(Integer.parseInt(endParts[1]));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                timeRow.addView(tpStart);
                timeRow.addView(tpEnd);

                Button btnSave = new Button(this);
                btnSave.setText("SAVE SCHEDULE");
                btnSave.setTextColor(0xFFFFFFFF);
                btnSave.setTextSize(12f);
                btnSave.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF1A3A8F));
                btnSave.setOnClickListener(v -> {
                    String start = String.format(Locale.getDefault(),
                            "%02d:%02d",
                            tpStart.getHour(), tpStart.getMinute());
                    String end = String.format(Locale.getDefault(),
                            "%02d:%02d",
                            tpEnd.getHour(), tpEnd.getMinute());
                    db.updateTeacherSchedule(teacherId, start, end);
                    Toast.makeText(this,
                            "Schedule updated for " + name,
                            Toast.LENGTH_SHORT).show();
                    tvSched.setText("Current schedule: " +
                            start + " – " + end);
                });

                card.addView(tvName);
                card.addView(tvSched);
                card.addView(timeRow);
                card.addView(btnSave);
                llScheduleList.addView(card);

            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void switchTab(int tab) {
        layoutToday.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        layoutMonthly.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        layoutSchedules.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);

        int red = 0xFFCC0000;
        int gray = 0xFFF5F5F5;

        btnTabToday.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        tab == 0 ? red : gray));
        btnTabToday.setTextColor(tab == 0 ? 0xFFFFFFFF : 0xFFCC0000);

        btnTabMonthly.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        tab == 1 ? red : gray));
        btnTabMonthly.setTextColor(tab == 1 ? 0xFFFFFFFF : 0xFFCC0000);

        btnTabSchedules.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        tab == 2 ? red : gray));
        btnTabSchedules.setTextColor(tab == 2 ? 0xFFFFFFFF : 0xFFCC0000);
    }
}