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
    TextView tvAttendanceDate;
    LinearLayout navHome, navAttendance, navScanner, navReports, navProfile;
    int userId;
    String today;
    java.util.Calendar calendar = java.util.Calendar.getInstance();

    List<ApiService.ClassModel> classesList = new ArrayList<>();
    List<String> classDisplayNames = new ArrayList<>();

    java.util.Map<Integer, String> sessionAttendance = new java.util.HashMap<>();

    int selectedClassId = -1;

    java.util.Map<Integer, Integer> seatMapping = new java.util.HashMap<>();
    List<ApiService.StudentModel> currentStudents = new ArrayList<>();

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
        tvAttendanceDate = findViewById(R.id.tvAttendanceDate);

        navHome      = findViewById(R.id.navHome);
        navAttendance = findViewById(R.id.navAttendance);
        navScanner   = findViewById(R.id.navScanner);
        navReports   = findViewById(R.id.navReports);
        navProfile   = findViewById(R.id.navProfile);

        findViewById(R.id.llDatePicker).setOnClickListener(v -> showDatePicker());
        updateDateDisplay();

        setupNavigation();
        loadClassesOnline();

        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedClassId = -1;
                    sessionAttendance.clear();
                    seatMapping.clear();
                    gridSeating.removeAllViews();
                } else {
                    selectedClassId = classesList.get(position - 1).id;
                    sessionAttendance.clear();
                    loadMappingFromPrefs();
                    loadTodayAttendanceThenSeating();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateDateDisplay() {
        tvAttendanceDate.setText(new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(calendar.getTime()));
        today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
    }

    private void showDatePicker() {
        new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            updateDateDisplay();
            sessionAttendance.clear();
            if (selectedClassId != -1) {
                loadTodayAttendanceThenSeating();
            }
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectedClassId != -1) {
            loadTodayAttendanceThenSeating();
        }
    }

    private void loadMappingFromPrefs() {
        seatMapping.clear();
        android.content.SharedPreferences prefs = getSharedPreferences("seating_arrangements", MODE_PRIVATE);
        
        String key = "class_" + selectedClassId + "_" + today;
        String saved = prefs.getString(key, "");
        
        if (!saved.isEmpty()) {
            String[] pairs = saved.split(",");
            for (String p : pairs) {
                String[] parts = p.split(":");
                if (parts.length == 2) {
                    seatMapping.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                }
            }
        }
    }

    private void saveMappingToPrefs() {
        if (selectedClassId == -1) return;
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<Integer, Integer> entry : seatMapping.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        
        getSharedPreferences("seating_arrangements", MODE_PRIVATE)
                .edit()
                .putString("class_" + selectedClassId + "_" + today, sb.toString())
                .apply();
    }

    private void loadTodayAttendanceThenSeating() {
        if (selectedClassId == -1) return;

        sessionAttendance.clear(); 
        loadMappingFromPrefs(); 

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
                        loadSeatingArrangementOnline();
                    }

                    @Override
                    public void onFailure(Call<List<ApiService.AttendanceRecord>> call, Throwable t) {
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
                android.util.Log.e("SeatingActivity", "Failed to load classes", t);
                Toast.makeText(SeatingActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
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
                            currentStudents = response.body();
                            renderSeatingGrid();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ApiService.StudentModel>> call, Throwable t) {
                        Toast.makeText(SeatingActivity.this, "Failed to load students",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderSeatingGrid() {
        gridSeating.removeAllViews();
        if (currentStudents.isEmpty()) return;

        int totalGridSize = Math.max(currentStudents.size() + 10, 30); 
        int totalCols = 5;
        int rows = (int) Math.ceil((double) totalGridSize / totalCols);

        int screenWidth  = getResources().getDisplayMetrics().widthPixels;
        float density    = getResources().getDisplayMetrics().density;
        int paddingTotal = (int) (50 * density);
        int seatWidth    = (screenWidth - paddingTotal) / totalCols;
        int seatHeight   = (int) (65 * density);
        int aisleWidth   = (int) (20 * density);

        java.util.Map<Integer, ApiService.StudentModel> studentLookup = new java.util.HashMap<>();
        java.util.Set<Integer> assignedStudents = new java.util.HashSet<>();
        for (ApiService.StudentModel sm : currentStudents) {
            studentLookup.put(sm.id, sm);
        }

        java.util.Iterator<java.util.Map.Entry<Integer, Integer>> it = seatMapping.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<Integer, Integer> entry = it.next();
            if (!studentLookup.containsKey(entry.getValue())) {
                it.remove();
            } else {
                assignedStudents.add(entry.getValue());
            }
        }

        int currentSlot = 0;
        for (ApiService.StudentModel sm : currentStudents) {
            if (!assignedStudents.contains(sm.id)) {
                while (currentSlot % totalCols == 2 || seatMapping.containsKey(currentSlot)) {
                    currentSlot++;
                }
                seatMapping.put(currentSlot, sm.id);
                assignedStudents.add(sm.id);
            }
        }

        for (int i = 0; i < rows * totalCols; i++) {
            int col = i % totalCols;
            if (col == 2) {
                View aisle = new View(this);
                aisle.setLayoutParams(new ViewGroup.LayoutParams(aisleWidth, seatHeight));
                gridSeating.addView(aisle);
                continue;
            }

            final int gridIndex = i;
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

            Integer studentId = seatMapping.get(gridIndex);
            if (studentId != null && studentLookup.containsKey(studentId)) {
                final ApiService.StudentModel sm = studentLookup.get(studentId);
                tvSeat.setText(extractLastName(sm.name).toUpperCase());
                tvSeat.setTypeface(null, android.graphics.Typeface.BOLD);

                String status = sessionAttendance.get(sm.id);
                if (status == null) {
                    android.content.SharedPreferences prefs = getSharedPreferences("AttendancePrefs", MODE_PRIVATE);
                    status = prefs.getString("status_" + sm.id + "_" + today, "");
                }
                
                applyStatusColor(tvSeat, status);

                tvSeat.setOnClickListener(v -> showAttendanceOptions(sm));
                
                tvSeat.setOnLongClickListener(v -> {
                    android.content.ClipData data = android.content.ClipData.newPlainText("index", String.valueOf(gridIndex));
                    View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                    v.startDragAndDrop(data, shadow, v, 0);
                    return true;
                });
            } else {
                tvSeat.setText("EMPTY");
                tvSeat.setBackgroundColor(0xFFFAFAFA);
                tvSeat.setTextColor(0xFFD1D5DB);
            }

            tvSeat.setOnDragListener((v, event) -> {
                if (event.getAction() == android.view.DragEvent.ACTION_DROP) {
                    int fromIndex = Integer.parseInt(event.getClipData().getItemAt(0).getText().toString());
                    int toIndex = gridIndex;

                    if (fromIndex != toIndex) {
                        Integer studentFrom = seatMapping.get(fromIndex);
                        Integer studentTo = seatMapping.get(toIndex);

                        if (studentFrom != null) {
                            seatMapping.put(toIndex, studentFrom);
                        } else {
                            seatMapping.remove(toIndex);
                        }

                        if (studentTo != null) {
                            seatMapping.put(fromIndex, studentTo);
                        } else {
                            seatMapping.remove(fromIndex);
                        }

                        saveMappingToPrefs();
                        renderSeatingGrid();
                    }
                }
                return true;
            });

            gridSeating.addView(tvSeat);
        }
    }

    private void applyStatusColor(TextView tv, String status) {
        if ("Present".equalsIgnoreCase(status)) {
            tv.setBackgroundColor(0xFF16A34A); // Green
            tv.setTextColor(Color.WHITE);
        } else if ("Absent".equalsIgnoreCase(status)) {
            tv.setBackgroundColor(0xFFCC0000); // Red
            tv.setTextColor(Color.WHITE);
        } else if ("Late".equalsIgnoreCase(status)) {
            tv.setBackgroundColor(0xFFD97706); // Amber
            tv.setTextColor(Color.WHITE);
        } else {
            tv.setBackgroundColor(0xFFF3F4F6); // Light Gray
            tv.setTextColor(0xFF1F2937);
        }
    }

    private void showAttendanceOptions(ApiService.StudentModel sm) {
        String[] options = {"Present", "Absent", "Late", "Clear"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Mark Attendance for " + sm.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 3) {
                        sessionAttendance.remove(sm.id);
                        getSharedPreferences("AttendancePrefs", MODE_PRIVATE).edit()
                                .remove("status_" + sm.id + "_" + today).apply();
                        renderSeatingGrid();
                    } else {
                        String status = options[which];
                        markAttendanceOnline(sm, status);
                    }
                }).show();
    }

    private void markAttendanceOnline(ApiService.StudentModel sm, String status) {
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
                            
                            android.content.SharedPreferences prefs = getSharedPreferences("AttendancePrefs", MODE_PRIVATE);
                            prefs.edit().putString("status_" + sm.id + "_" + today, status).apply();

                            renderSeatingGrid();
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