package com.example.attendancetrackerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    TextView tvWelcome, tvDate, tvPresentCount, tvAbsentCount, tvLateCount, tvRateCount;
    LinearLayout llClassList;
    LinearLayout navHome, navAttendance, navScanner, navReports, navProfile;
    int userId;
    String todayDb;

    List<ApiService.ClassModel> classesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        todayDb = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String todayDisplay = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date());

        tvWelcome = findViewById(R.id.tvWelcome);
        tvDate = findViewById(R.id.tvDate);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        tvLateCount = findViewById(R.id.tvLateCount);
        tvRateCount = findViewById(R.id.tvRateCount);
        llClassList = findViewById(R.id.llClassList);

        navHome = findViewById(R.id.navHome);
        navAttendance = findViewById(R.id.navAttendance);
        navScanner = findViewById(R.id.navScanner);
        navReports = findViewById(R.id.navReports);
        navProfile = findViewById(R.id.navProfile);

        tvDate.setText(todayDisplay);

        String userName = prefs.getString("name", "Teacher");
        tvWelcome.setText(String.format("Good day, %s!", userName));

        setupNavigation();
        loadClassesOnline();
        loadOverallSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClassesOnline();
        loadOverallSummary();
    }

    private void loadOverallSummary() {
        ApiService.create().getSummary(userId, todayDb, null).enqueue(new Callback<ApiService.SummaryModel>() {
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
                }
            }
            @Override
            public void onFailure(Call<ApiService.SummaryModel> call, Throwable t) {}
        });
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
                Toast.makeText(HomeActivity.this, "Failed to load classes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderClassList() {
        llClassList.removeAllViews();
        if (classesList.isEmpty()) {
            TextView tvNone = new TextView(this);
            tvNone.setText("No classes yet. Go to Attendance to add one.");
            tvNone.setPadding(32, 10, 0, 10);
            llClassList.addView(tvNone);
            return;
        }

        for (ApiService.ClassModel cm : classesList) {
            addClassCard(cm.id, cm.subject_name, cm.section);
        }
    }

    private void addClassCard(int id, String title, String subtitle) {
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

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, ClassSummaryActivity.class);
            intent.putExtra("class_id", id);
            intent.putExtra("class_name", title + " - " + subtitle);
            startActivity(intent);
        });

        llClassList.addView(card);
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> {
            loadClassesOnline();
            loadOverallSummary();
        });
        navAttendance.setOnClickListener(v -> startActivity(new Intent(this, AttendanceActivity.class)));
        navScanner.setOnClickListener(v -> startActivity(new Intent(this, SeatingActivity.class)));
        navReports.setOnClickListener(v -> startActivity(new Intent(this, ReportsActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }
}
