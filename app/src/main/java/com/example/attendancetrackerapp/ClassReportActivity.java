package com.example.attendancetrackerapp;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClassReportActivity extends AppCompatActivity {

    LinearLayout llReportList, llFlagged;
    int userId, classId;
    String className;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_report);

        classId = getIntent().getIntExtra("class_id", -1);
        className = getIntent().getStringExtra("class_name");

        android.content.SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        llReportList = findViewById(R.id.llReportList);
        llFlagged = findViewById(R.id.llFlagged);
        TextView tvTitle = findViewById(R.id.tvReportTitle);
        tvTitle.setText(className);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadReportsOnline();
    }

    private void loadReportsOnline() {
        Integer apiClassId = classId == -1 ? null : classId;
        ApiService.create().getReports(userId, apiClassId).enqueue(new Callback<List<ApiService.ReportModel>>() {
            @Override
            public void onResponse(Call<List<ApiService.ReportModel>> call, Response<List<ApiService.ReportModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (classId != -1) {
                        filterReportsByClass(response.body());
                    } else {
                        renderReportsUI(response.body());
                    }
                }
            }
            @Override
            public void onFailure(Call<List<ApiService.ReportModel>> call, Throwable t) {
                Toast.makeText(ClassReportActivity.this, "Failed to load reports", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterReportsByClass(List<ApiService.ReportModel> globalReports) {
        ApiService.create().getStudents(classId).enqueue(new Callback<List<ApiService.StudentModel>>() {
            @Override
            public void onResponse(Call<List<ApiService.StudentModel>> call, Response<List<ApiService.StudentModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ApiService.StudentModel> classStudents = response.body();
                    List<ApiService.ReportModel> filteredList = new java.util.ArrayList<>();
                    for (ApiService.ReportModel rm : globalReports) {
                        for (ApiService.StudentModel sm : classStudents) {
                            if (rm.student_number.equals(sm.student_number)) {
                                filteredList.add(rm);
                                break;
                            }
                        }
                    }
                    renderReportsUI(filteredList);
                } else {
                    renderReportsUI(globalReports);
                }
            }
            @Override public void onFailure(Call<List<ApiService.StudentModel>> call, Throwable t) {
                renderReportsUI(globalReports);
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
            tvNone.setText("No flagged students in this class.");
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
}
