package com.example.attendancetrackerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportsActivity extends AppCompatActivity {

    LinearLayout llReportList, llFlagged, llClassList;
    LinearLayout navHome, navAttendance, navScanner, navReports, navProfile;
    int userId;
    List<ApiService.ClassModel> classesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        llReportList = findViewById(R.id.llReportList);
        llFlagged = findViewById(R.id.llFlagged);
        llClassList = findViewById(R.id.llClassList);
        navHome = findViewById(R.id.navHome);
        navAttendance = findViewById(R.id.navAttendance);
        navScanner = findViewById(R.id.navScanner);
        navReports = findViewById(R.id.navReports);
        navProfile = findViewById(R.id.navProfile);

        setupNavigation();
        loadClassesOnline();
        loadOverallReports();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClassesOnline();
        loadOverallReports();
    }

    private void loadOverallReports() {
        ApiService.create().getReports(userId, null).enqueue(new Callback<List<ApiService.ReportModel>>() {
            @Override
            public void onResponse(Call<List<ApiService.ReportModel>> call, Response<List<ApiService.ReportModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    renderReportsUI(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<ApiService.ReportModel>> call, Throwable t) {}
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
            tvNone.setText("No flagged students overall.");
            tvNone.setTextColor(0xFF16A34A);
            llFlagged.addView(tvNone);
        }
    }

    private TextView createSmallStat(String text, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(12f);
        tv.setPadding(0, 0, 24, 0);
        return tv;
    }

    private void loadClassesOnline() {
        ApiService.create().getClasses(userId).enqueue(new Callback<List<ApiService.ClassModel>>() {
            @Override
            public void onResponse(Call<List<ApiService.ClassModel>> call, Response<List<ApiService.ClassModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    classesList = response.body();
                    renderClassList();
                }
            }
            @Override
            public void onFailure(Call<List<ApiService.ClassModel>> call, Throwable t) {
                Toast.makeText(ReportsActivity.this, "Failed to load classes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderClassList() {
        llClassList.removeAllViews();
        if (classesList.isEmpty()) {
            TextView tvNone = new TextView(this);
            tvNone.setText("No classes yet.");
            llClassList.addView(tvNone);
            return;
        }

        for (ApiService.ClassModel cm : classesList) {
            addClassCard(cm.id, cm.subject_name, cm.section, cm.schedule);
        }
    }

    private void addClassCard(int id, String title, String subtitle, String schedule) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFF5F5F5);
        card.setPadding(32, 24, 32, 24);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, 16);
        card.setLayoutParams(lp);
        card.setElevation(2f);

        TextView tvSubject = new TextView(this);
        tvSubject.setText(title);
        tvSubject.setTextSize(16f);
        tvSubject.setTypeface(null, android.graphics.Typeface.BOLD);
        tvSubject.setTextColor(0xFF000000);

        TextView tvSection = new TextView(this);
        tvSection.setText("Section: " + subtitle);
        tvSection.setTextSize(13f);
        tvSection.setTextColor(0xFF555555);

        card.addView(tvSubject);
        card.addView(tvSection);

        if (schedule != null && !schedule.isEmpty()) {
            TextView tvSched = new TextView(this);
            tvSched.setText("Schedule: " + schedule);
            tvSched.setTextSize(12f);
            tvSched.setTextColor(0xFF777777);
            card.addView(tvSched);
        }

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, ClassReportActivity.class);
            intent.putExtra("class_id", id);
            intent.putExtra("class_name", title + " - " + subtitle);
            startActivity(intent);
        });

        llClassList.addView(card);
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        navAttendance.setOnClickListener(v -> startActivity(new Intent(this, AttendanceActivity.class)));
        navScanner.setOnClickListener(v -> startActivity(new Intent(this, SeatingActivity.class)));
        navReports.setOnClickListener(v -> {
            loadClassesOnline();
            loadOverallReports();
        });
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }
}
