package com.example.attendancetrackerapp;

import android.content.Intent;
import android.graphics.Color;
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

public class SeatingActivity extends AppCompatActivity {

    GridLayout gridSeating;
    Spinner spinnerClass;
    LinearLayout navHome, navAttendance, navScanner, navReports, navProfile;
    int userId;
    String today;

    List<ApiService.ClassModel> classesList = new ArrayList<>();
    List<String> classDisplayNames = new ArrayList<>();

    // ── Attendance map is now loaded from the server each time a class is selected ──
    java.util.Map<Integer, String> sessionAttendance = new java.util.HashMap<>();

    int selectedClassId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seating);

        android.content.SharedPreferences prefs = getSharedPreferences(
                "user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        gridSeating = findViewById(R.id.gridSeating);
        spinnerClass = findViewById(R.id.spinnerClass);

        navHome      = findViewById(R.id.navHome);
        navAttendance = findViewById(R.id.navAttendance);
        navScanner   = findViewById(R.id.navScanner);
        navReports   = findViewById(R.id.navReports);
        navProfile   = findViewById(R.id.navProfile);

        setupNavigation();
        loadClassesOnline();

        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedClassId = -1;
                    sessionAttendance.clear();
                    gridSeating.removeAllViews();
                } else {
                    selectedClassId = classesList.get(position - 1).id;
                    sessionAttendance.clear();
                    // ── Load today's saved attendance FIRST, then render the grid ──
                    loadTodayAttendanceThenSeating();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ── NEW: fetch today's attendance records for this class, populate
    //         sessionAttendance, then load the seating grid ──────────────────────
    private void loadTodayAttendanceThenSeating() {
        if (selectedClassId == -1) return;

        ApiService.create()
                .getTodayAttendance(selectedClassId, today)
                .enqueue(new Callback<List<ApiService.AttendanceRecord>>() {
                    @Override
                    public void onResponse(Call<List<ApiService.AttendanceRecord>> call,
                                           Response<List<ApiService.AttendanceRecord>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            for (ApiService.AttendanceRecord r : response.body()) {
                                sessionAttendance.put(r.student_id, r.status);
                            }
                        }
                        // Render grid regardless (even if fetch failed, we still show seats)
                        loadSeatingArrangementOnline();
                    }

                    @Override
                    public void onFailure(Call<List<ApiService.AttendanceRecord>> call, Throwable t) {
                        // Still show the seating grid even if attendance fetch fails
                        loadSeatingArrangementOnline();
                    }
                });
    }

    private void loadClassesOnline() {
        ApiService.create().getClasses(userId).enqueue(new Callback<List<ApiService.ClassModel>>() {
            @Override
            public void onResponse(Call<List<ApiService.ClassModel>> call,
                                   Response<List<ApiService.ClassModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    classesList = response.body();
                    classDisplayNames.clear();
                    classDisplayNames.add("-- Select Class --");
                    for (ApiService.ClassModel c : classesList) {
                        // Show schedule in spinner if available
                        String display = c.subject_name + " - " + c.section;
                        if (c.schedule != null && !c.schedule.isEmpty()) {
                            display += " (" + c.schedule + ")";
                        }
                        classDisplayNames.add(display);
                    }
                    updateSpinner();
                }
            }

            @Override
            public void onFailure(Call<List<ApiService.ClassModel>> call, Throwable t) {
                Toast.makeText(SeatingActivity.this, "Failed to load classes", Toast.LENGTH_SHORT).show();
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
            public View getDropDownView(int position, @Nullable View convertView,
                                        @NonNull ViewGroup parent) {
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

        ApiService.create().getStudents(selectedClassId)
                .enqueue(new Callback<List<ApiService.StudentModel>>() {
                    @Override
                    public void onResponse(Call<List<ApiService.StudentModel>> call,
                                           Response<List<ApiService.StudentModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            renderSeatingGrid(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ApiService.StudentModel>> call, Throwable t) {
                        Toast.makeText(SeatingActivity.this, "Failed to load students",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderSeatingGrid(List<ApiService.StudentModel> students) {
        gridSeating.removeAllViews();

        int totalStudents = students.size();
        if (totalStudents == 0) return;

        int screenWidth  = getResources().getDisplayMetrics().widthPixels;
        float density    = getResources().getDisplayMetrics().density;
        int paddingTotal = (int) (50 * density);
        int totalCols    = 5;
        int seatWidth    = (screenWidth - paddingTotal) / totalCols;
        int seatHeight   = (int) (65 * density);
        int aisleWidth   = (int) (20 * density);

        int studentsPerRow = 4;
        int rows = (int) Math.ceil((double) totalStudents / studentsPerRow);
        rows = Math.max(rows, 5);

        int studentIdx = 0;
        for (int i = 0; i < rows * totalCols; i++) {
            int col = i % totalCols;
            if (col == 2) {
                View aisle = new View(this);
                aisle.setLayoutParams(new ViewGroup.LayoutParams(aisleWidth, seatHeight));
                gridSeating.addView(aisle);
                continue;
            }

            TextView tvSeat = new TextView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width  = seatWidth;
            params.height = seatHeight;
            params.setMargins(4, 8, 4, 8);
            tvSeat.setLayoutParams(params);
            tvSeat.setElevation(2f);
            tvSeat.setGravity(Gravity.CENTER);
            tvSeat.setTextSize(10f);
            tvSeat.setPadding(4, 4, 4, 4);

            if (studentIdx < totalStudents) {
                final ApiService.StudentModel sm = students.get(studentIdx);
                tvSeat.setText(extractLastName(sm.name).toUpperCase());

                // Color coding — uses the map populated from the server ──────────
                String status = sessionAttendance.get(sm.id);
                if ("Present".equalsIgnoreCase(status)) {
                    tvSeat.setBackgroundColor(0xFF16A34A); // Green
                    tvSeat.setTextColor(Color.WHITE);
                } else if ("Absent".equalsIgnoreCase(status)) {
                    tvSeat.setBackgroundColor(0xFFCC0000); // Red
                    tvSeat.setTextColor(Color.WHITE);
                } else if ("Late".equalsIgnoreCase(status)) {
                    tvSeat.setBackgroundColor(0xFFD97706); // Amber/Yellow
                    tvSeat.setTextColor(Color.WHITE);
                } else {
                    tvSeat.setBackgroundColor(0xFFF3F4F6); // Default Grey
                    tvSeat.setTextColor(0xFF1F2937);
                }

                tvSeat.setTypeface(null, android.graphics.Typeface.BOLD);
                tvSeat.setOnClickListener(v -> showAttendanceOptions(sm, students));
                studentIdx++;
            } else {
                tvSeat.setText("EMPTY");
                tvSeat.setBackgroundColor(0xFFFAFAFA);
                tvSeat.setTextColor(0xFFD1D5DB);
            }
            gridSeating.addView(tvSeat);
        }
    }

    private void showAttendanceOptions(ApiService.StudentModel sm,
                                       List<ApiService.StudentModel> currentStudents) {
        String[] options = {"Present", "Absent", "Late"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Mark Attendance for " + sm.name)
                .setItems(options, (dialog, which) -> {
                    String status = options[which];
                    markAttendanceOnline(sm, status, currentStudents);
                }).show();
    }

    private void markAttendanceOnline(ApiService.StudentModel sm, String status,
                                      List<ApiService.StudentModel> currentStudents) {
        android.content.SharedPreferences prefs =
                getSharedPreferences("user_session", MODE_PRIVATE);
        String tName    = prefs.getString("name", "Teacher");
        String className = spinnerClass.getSelectedItem().toString();

        ApiService.create()
                .markAttendance(sm.id, sm.name, className, userId, tName, today, status, selectedClassId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call,
                                           Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(SeatingActivity.this,
                                    sm.name + " marked as " + status, Toast.LENGTH_SHORT).show();
                            sessionAttendance.put(sm.id, status);
                            renderSeatingGrid(currentStudents);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(SeatingActivity.this, "Failed to mark attendance",
                                Toast.LENGTH_SHORT).show();
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
        navScanner.setOnClickListener(v -> {
            if (selectedClassId != -1) loadTodayAttendanceThenSeating();
        });
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