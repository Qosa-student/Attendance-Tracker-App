package com.example.attendancetrackerapp;

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

public class ClassSummaryActivity extends AppCompatActivity {

    TextView tvClassTitle, tvDate, tvPresentCount, tvAbsentCount, tvLateCount, tvRateCount;
    LinearLayout llStudentAttendanceList;
    int userId, classId;
    String className;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_summary);

        classId = getIntent().getIntExtra("class_id", -1);
        className = getIntent().getStringExtra("class_name");

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        String todayDisplay = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date());

        tvClassTitle = findViewById(R.id.tvClassTitle);
        tvDate = findViewById(R.id.tvDate);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        tvLateCount = findViewById(R.id.tvLateCount);
        tvRateCount = findViewById(R.id.tvRateCount);
        llStudentAttendanceList = findViewById(R.id.llStudentAttendanceList);

        tvClassTitle.setText(className);
        tvDate.setText(todayDisplay);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadOverallClassData();
    }

    private void loadOverallClassData() {
        if (classId == -1) {
            fetchReports(null);
        } else {
            ApiService.create().getStudents(classId).enqueue(new Callback<List<ApiService.StudentModel>>() {
                @Override
                public void onResponse(Call<List<ApiService.StudentModel>> call, Response<List<ApiService.StudentModel>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        fetchReports(response.body());
                    }
                }
                @Override
                public void onFailure(Call<List<ApiService.StudentModel>> call, Throwable t) {
                    Toast.makeText(ClassSummaryActivity.this, "Failed to load class roster", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void fetchReports(List<ApiService.StudentModel> classStudents) {
        Integer apiClassId = (classId == -1) ? null : classId;
        ApiService.create().getReports(userId, apiClassId).enqueue(new Callback<List<ApiService.ReportModel>>() {
            @Override
            public void onResponse(Call<List<ApiService.ReportModel>> call, Response<List<ApiService.ReportModel>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    processOverallData(classStudents, response.body());
                } else {
                    fetchGlobalReports(classStudents);
                }
            }
            @Override
            public void onFailure(Call<List<ApiService.ReportModel>> call, Throwable t) {
                fetchGlobalReports(classStudents);
            }
        });
    }

    private void fetchGlobalReports(List<ApiService.StudentModel> classStudents) {
        ApiService.create().getReports(userId, null).enqueue(new Callback<List<ApiService.ReportModel>>() {
            @Override
            public void onResponse(Call<List<ApiService.ReportModel>> call, Response<List<ApiService.ReportModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processOverallData(classStudents, response.body());
                }
            }
            @Override
            public void onFailure(Call<List<ApiService.ReportModel>> call, Throwable t) {
                Toast.makeText(ClassSummaryActivity.this, "Failed to load reports", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processOverallData(List<ApiService.StudentModel> classStudents, List<ApiService.ReportModel> reports) {
        List<ApiService.ReportModel> finalReports = new ArrayList<>();

        int totalPresent = 0;
        int totalAbsent = 0;
        int totalLate = 0;

        if (classId == -1) {
            finalReports = reports;
        } else {
            for (ApiService.StudentModel sm : classStudents) {
                ApiService.ReportModel match = null;
                for (ApiService.ReportModel rm : reports) {
                    if (rm.student_number != null && sm.student_number != null &&
                            rm.student_number.trim().equals(sm.student_number.trim())) {
                        match = rm;
                        break;
                    }
                    if (rm.name != null && sm.name != null &&
                            rm.name.trim().equalsIgnoreCase(sm.name.trim())) {
                        match = rm;
                    }
                }

                if (match == null) {
                    match = new ApiService.ReportModel();
                    match.name = sm.name;
                    match.student_number = sm.student_number;
                    match.present_count = 0;
                    match.absent_count = 0;
                    match.late_count = 0;
                    match.total_count = 0;
                }
                finalReports.add(match);
            }
        }

        for (ApiService.ReportModel rm : finalReports) {
            totalPresent += rm.present_count;
            totalAbsent += rm.absent_count;
            totalLate += rm.late_count;
        }

        tvPresentCount.setText(String.valueOf(totalPresent));
        tvAbsentCount.setText(String.valueOf(totalAbsent));
        tvLateCount.setText(String.valueOf(totalLate));

        int totalDays = totalPresent + totalAbsent + totalLate;
        int rate = totalDays > 0 ? Math.round((totalPresent * 100f) / totalDays) : 0;
        tvRateCount.setText(String.format(Locale.getDefault(), "%d%%", rate));

        renderStudentAttendanceList(finalReports);
    }

    private void renderStudentAttendanceList(List<ApiService.ReportModel> reports) {
        llStudentAttendanceList.removeAllViews();

        if (reports == null || reports.isEmpty()) {
            TextView tvNone = new TextView(this);
            tvNone.setText("No students found.");
            tvNone.setPadding(16, 16, 16, 16);
            llStudentAttendanceList.addView(tvNone);
            return;
        }

        for (ApiService.ReportModel r : reports) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 12, 0, 12);

            // Bottom divider
            row.setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            }));

            TextView tvName = new TextView(this);
            tvName.setText(r.name);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvName.setTextSize(14f);
            tvName.setTextColor(0xFF000000);

            TextView tvId = new TextView(this);
            tvId.setText(r.student_number);
            tvId.setTextColor(0xFF888888);
            tvId.setTextSize(12f);

            // P / A / L stats in a horizontal row
            LinearLayout statsRow = new LinearLayout(this);
            statsRow.setOrientation(LinearLayout.HORIZONTAL);
            statsRow.setPadding(0, 4, 0, 0);

            statsRow.addView(createStatTextView("P: " + r.present_count, 0xFF16A34A));
            statsRow.addView(createStatTextView("A: " + r.absent_count, 0xFFCC0000));
            statsRow.addView(createStatTextView("L: " + r.late_count, 0xFFD97706));

            row.addView(tvName);
            row.addView(tvId);
            row.addView(statsRow);

            // Divider line
            android.view.View divider = new android.view.View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(-1, 1));
            divider.setBackgroundColor(0xFFEEEEEE);
            row.addView(divider);

            llStudentAttendanceList.addView(row);
        }
    }

    private TextView createStatTextView(String text, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, 0, 32, 0);
        tv.setTextSize(14f);
        return tv;
    }
}