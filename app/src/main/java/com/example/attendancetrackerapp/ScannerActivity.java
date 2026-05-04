package com.example.attendancetrackerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScannerActivity extends AppCompatActivity {

    GridLayout gridSeating;
    Spinner spinnerClass;
    LinearLayout navHome, navAttendance, navScanner, navReports, navProfile;
    int userId;
    String today;
    
    List<ApiService.ClassModel> classesList = new ArrayList<>();
    List<String> classDisplayNames = new ArrayList<>();
    int selectedClassId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        android.content.SharedPreferences prefs = getSharedPreferences(
                "user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        
        today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        gridSeating = findViewById(R.id.gridSeating);
        spinnerClass = findViewById(R.id.spinnerClass);

        navHome = findViewById(R.id.navHome);
        navAttendance = findViewById(R.id.navAttendance);
        navScanner = findViewById(R.id.navScanner);
        navReports = findViewById(R.id.navReports);
        navProfile = findViewById(R.id.navProfile);

        setupNavigation();
        loadClassesOnline();

        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedClassId = -1;
                    gridSeating.removeAllViews();
                } else {
                    selectedClassId = classesList.get(position - 1).id;
                    loadSeatingArrangementOnline();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadClassesOnline() {
        ApiService.create().getClasses(userId).enqueue(new Callback<List<ApiService.ClassModel>>() {
            @Override
            public void onResponse(Call<List<ApiService.ClassModel>> call, Response<List<ApiService.ClassModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    classesList = response.body();
                    classDisplayNames.clear();
                    classDisplayNames.add("-- Select Class --");
                    for (ApiService.ClassModel c : classesList) {
                        classDisplayNames.add(c.subject_name + " - " + c.section);
                    }
                    updateSpinner();
                }
            }

            @Override
            public void onFailure(Call<List<ApiService.ClassModel>> call, Throwable t) {
                Toast.makeText(ScannerActivity.this, "Failed to load classes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, classDisplayNames) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? 0xFF999999 : 0xFF000000);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClass.setAdapter(adapter);
    }

    private String extractLastName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "Empty";
        if (fullName.contains(",")) return fullName.split(",")[0].trim();
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 1 ? parts[parts.length - 1] : fullName;
    }

    private void loadSeatingArrangementOnline() {
        gridSeating.removeAllViews();
        if (selectedClassId == -1) return;

        ApiService.create().getStudents(selectedClassId).enqueue(new Callback<List<ApiService.StudentModel>>() {
            @Override
            public void onResponse(Call<List<ApiService.StudentModel>> call, Response<List<ApiService.StudentModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    renderSeatingGrid(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<ApiService.StudentModel>> call, Throwable t) {
                Toast.makeText(ScannerActivity.this, "Failed to load students", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderSeatingGrid(List<ApiService.StudentModel> students) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        float density = getResources().getDisplayMetrics().density;
        int paddingTotal = (int) (50 * density); 
        int totalCols = 5; 
        int seatWidth = (screenWidth - paddingTotal) / totalCols;
        int seatHeight = (int) (65 * density);
        int aisleWidth = (int) (20 * density);

        int studentIdx = 0;
        for (int i = 0; i < 25; i++) {
            int col = i % 5;
            if (col == 2) {
                View aisle = new View(this);
                aisle.setLayoutParams(new ViewGroup.LayoutParams(aisleWidth, seatHeight));
                gridSeating.addView(aisle);
                continue;
            }

            TextView tvSeat = new TextView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = seatWidth;
            params.height = seatHeight;
            params.setMargins(4, 8, 4, 8);
            tvSeat.setLayoutParams(params);
            tvSeat.setElevation(2f);
            tvSeat.setGravity(Gravity.CENTER);
            tvSeat.setTextSize(10f);
            tvSeat.setPadding(4, 4, 4, 4);

            if (studentIdx < students.size() && studentIdx < 20) {
                final ApiService.StudentModel sm = students.get(studentIdx);
                tvSeat.setText(extractLastName(sm.name).toUpperCase());
                tvSeat.setBackgroundColor(0xFFF3F4F6);
                tvSeat.setTextColor(0xFF1F2937);
                tvSeat.setTypeface(null, android.graphics.Typeface.BOLD);
                tvSeat.setOnClickListener(v -> showAttendanceOptions(sm));
                studentIdx++;
            } else {
                tvSeat.setText("EMPTY");
                tvSeat.setBackgroundColor(0xFFFAFAFA);
                tvSeat.setTextColor(0xFFD1D5DB);
            }
            gridSeating.addView(tvSeat);
        }
    }

    private void showAttendanceOptions(ApiService.StudentModel sm) {
        String[] options = {"Present", "Absent", "Late"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Mark Attendance for " + sm.name)
                .setItems(options, (dialog, which) -> {
                    String status = options[which].toLowerCase();
                    markAttendanceOnline(sm, status);
                }).show();
    }

    private void markAttendanceOnline(ApiService.StudentModel sm, String status) {
        android.content.SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String tName = prefs.getString("name", "Teacher");
        String className = spinnerClass.getSelectedItem().toString();

        ApiService.create().markAttendance(sm.id, sm.name, className, userId, tName, today, status)
                .enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ScannerActivity.this, "Marked as " + status, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ScannerActivity.this, "Failed to mark attendance", Toast.LENGTH_SHORT).show();
            }
        });
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
        navScanner.setOnClickListener(v -> { if (selectedClassId != -1) loadSeatingArrangementOnline(); });
        navReports.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
    }
}
