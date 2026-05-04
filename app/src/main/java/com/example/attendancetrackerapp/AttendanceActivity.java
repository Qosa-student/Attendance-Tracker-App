package com.example.attendancetrackerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceActivity extends AppCompatActivity {

    LinearLayout layoutClasses, layoutManual, layoutTypeId;
    LinearLayout llClassList, llStudentList;
    LinearLayout navHome, navAttendance, navScanner, navReports, navProfile;
    Button btnTabClasses, btnTabAttendance, btnTabTypeId;
    Button btnAddClass, btnSaveAttendance;
    Button btnMarkPresent, btnMarkAbsent, btnMarkLate;
    EditText etSubjectName, etSection, etSearch, etStudentId;
    TextView tvAttendanceDate;
    Spinner spinnerClass;
    int userId;
    int selectedClassId = -1;
    String today;
    HashMap<Integer, String> attendanceMap = new HashMap<>();
    HashMap<Integer, String> studentNamesCache = new HashMap<>();
    List<ApiService.ClassModel> classesList = new ArrayList<>();
    List<String> classDisplayNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        tvAttendanceDate = findViewById(R.id.tvAttendanceDate);
        tvAttendanceDate.setText(new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date()));

        layoutClasses = findViewById(R.id.layoutClasses);
        layoutManual = findViewById(R.id.layoutManual);
        layoutTypeId = findViewById(R.id.layoutTypeId);
        llClassList = findViewById(R.id.llClassList);
        llStudentList = findViewById(R.id.llStudentList);
        btnTabClasses = findViewById(R.id.btnTabClasses);
        btnTabAttendance = findViewById(R.id.btnTabAttendance);
        btnTabTypeId = findViewById(R.id.btnTabTypeId);
        btnAddClass = findViewById(R.id.btnAddClass);
        btnSaveAttendance = findViewById(R.id.btnSaveAttendance);
        btnMarkPresent = findViewById(R.id.btnMarkPresent);
        btnMarkAbsent = findViewById(R.id.btnMarkAbsent);
        btnMarkLate = findViewById(R.id.btnMarkLate);
        etSubjectName = findViewById(R.id.etSubjectName);
        etSection = findViewById(R.id.etSection);
        etSearch = findViewById(R.id.etSearch);
        etStudentId = findViewById(R.id.etStudentId);
        spinnerClass = findViewById(R.id.spinnerClass);

        navHome = findViewById(R.id.navHome);
        navAttendance = findViewById(R.id.navAttendance);
        navScanner = findViewById(R.id.navScanner);
        navReports = findViewById(R.id.navReports);
        navProfile = findViewById(R.id.navProfile);

        loadClassesOnline();
        setupTabs();
        setupTypeId();
        setupNavigation();

        btnAddClass.setOnClickListener(v -> {
            String subject = etSubjectName.getText().toString().trim();
            String section = etSection.getText().toString().trim();
            if (subject.isEmpty() || section.isEmpty()) {
                Toast.makeText(this, "Fill in subject and section", Toast.LENGTH_SHORT).show();
                return;
            }
            ApiService.create().addClass(subject, section, userId).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AttendanceActivity.this, "Class added!", Toast.LENGTH_SHORT).show();
                        etSubjectName.setText("");
                        etSection.setText("");
                        loadClassesOnline();
                    }
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(AttendanceActivity.this, "Failed to add class", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnSaveAttendance.setOnClickListener(v -> {
            if (attendanceMap.isEmpty()) {
                Toast.makeText(this, "No attendance marked", Toast.LENGTH_SHORT).show();
                return;
            }
            saveAttendanceOnline();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                renderStudentList(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadClassesOnline() {
        ApiService.create().getClasses(userId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<ApiService.ClassModel>> call, Response<List<ApiService.ClassModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    classesList = response.body();
                    updateClassListUI();
                    setupClassSpinner();
                }
            }
            @Override
            public void onFailure(Call<List<ApiService.ClassModel>> call, Throwable t) {
                Toast.makeText(AttendanceActivity.this, "Failed to load classes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateClassListUI() {
        llClassList.removeAllViews();
        if (classesList.isEmpty()) {
            TextView tvNone = new TextView(this);
            tvNone.setText("No classes yet.");
            llClassList.addView(tvNone);
            return;
        }

        for (ApiService.ClassModel cm : classesList) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(0xFFF5F5F5);
            card.setPadding(24, 16, 24, 16);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.setMargins(0, 0, 0, 10);
            card.setLayoutParams(cp);

            TextView tvSubject = new TextView(this);
            tvSubject.setText(cm.subject_name);
            tvSubject.setTypeface(null, android.graphics.Typeface.BOLD);
            
            TextView tvSection = new TextView(this);
            tvSection.setText("Section: " + cm.section);

            LinearLayout buttonRow = new LinearLayout(this);
            buttonRow.setOrientation(LinearLayout.HORIZONTAL);
            buttonRow.setPadding(0, 8, 0, 0);

            Button btnStudents = new Button(this);
            btnStudents.setText("Students");
            btnStudents.setOnClickListener(v -> {
                Intent intent = new Intent(this, ManageStudentsActivity.class);
                intent.putExtra("class_id", cm.id);
                intent.putExtra("class_name", cm.subject_name + " - " + cm.section);
                startActivity(intent);
            });

            Button btnDelete = new Button(this);
            btnDelete.setText("Delete");
            btnDelete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFCC0000));
            btnDelete.setTextColor(0xFFFFFFFF);
            btnDelete.setOnClickListener(v -> {
                deleteClassOnline(cm.id);
            });

            buttonRow.addView(btnStudents);
            buttonRow.addView(btnDelete);

            card.addView(tvSubject);
            card.addView(tvSection);
            card.addView(buttonRow);
            llClassList.addView(card);
        }
    }

    private void deleteClassOnline(int classId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Class")
                .setMessage("Delete this class and all its students?")
                .setPositiveButton("Delete", (d, w) -> {
                    ApiService.create().deleteClass(classId).enqueue(new Callback<>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AttendanceActivity.this, "Class deleted", Toast.LENGTH_SHORT).show();
                                loadClassesOnline();
                            }
                        }
                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupClassSpinner() {
        classDisplayNames.clear();
        classDisplayNames.add("-- Select Class --");
        for (ApiService.ClassModel cm : classesList) {
            classDisplayNames.add(cm.subject_name + " - " + cm.section);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, classDisplayNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClass.setAdapter(adapter);

        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedClassId = -1;
                    llStudentList.removeAllViews();
                } else {
                    selectedClassId = classesList.get(position - 1).id;
                    loadStudentsOnline();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private List<ApiService.StudentModel> currentStudents = new ArrayList<>();

    private void loadStudentsOnline() {
        if (selectedClassId == -1) return;
        ApiService.create().getStudents(selectedClassId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<ApiService.StudentModel>> call, Response<List<ApiService.StudentModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentStudents = response.body();
                    renderStudentList("");
                }
            }
            @Override
            public void onFailure(Call<List<ApiService.StudentModel>> call, Throwable t) {}
        });
    }

    private void renderStudentList(String query) {
        llStudentList.removeAllViews();
        for (ApiService.StudentModel sm : currentStudents) {
            if (!query.isEmpty() && !sm.name.toLowerCase().contains(query.toLowerCase()) && !sm.student_number.contains(query)) continue;
            
            studentNamesCache.put(sm.id, sm.name);
            
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvInfo = new TextView(this);
            tvInfo.setText(sm.name + " (" + sm.student_number + ")");
            tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

            LinearLayout buttons = new LinearLayout(this);
            String[] statuses = {"present", "absent", "late"};
            int[] colors = {0xFF16A34A, 0xFFCC0000, 0xFFD97706};
            String[] labels = {"P", "A", "L"};

            for (int i = 0; i < 3; i++) {
                final String status = statuses[i];
                Button btn = new Button(this);
                btn.setText(labels[i]);
                btn.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colors[i]));
                btn.setAlpha(attendanceMap.containsKey(sm.id) && status.equals(attendanceMap.get(sm.id)) ? 1f : 0.4f);
                btn.setOnClickListener(v -> {
                    attendanceMap.put(sm.id, status);
                    renderStudentList(etSearch.getText().toString());
                });
                buttons.addView(btn);
            }
            row.addView(tvInfo);
            row.addView(buttons);
            llStudentList.addView(row);
        }
    }

    private void saveAttendanceOnline() {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String tName = prefs.getString("name", "Teacher");
        String className = spinnerClass.getSelectedItem().toString();

        attendanceMap.forEach((studentId, status) -> {
            ApiService.create().markAttendance(studentId, studentNamesCache.get(studentId), 
                className, userId, tName, today, status).enqueue(new Callback<>() {
                    @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
                    @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                });
        });
        Toast.makeText(this, "Attendance sent to server!", Toast.LENGTH_SHORT).show();
        attendanceMap.clear();
        loadStudentsOnline();
    }

    private void setupTypeId() {
        etStudentId.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        btnMarkPresent.setOnClickListener(v -> Toast.makeText(this, "Please use seating or manual list", Toast.LENGTH_SHORT).show());
    }

    private void setupTabs() {
        btnTabClasses.setOnClickListener(v -> switchTab(0));
        btnTabAttendance.setOnClickListener(v -> switchTab(1));
        btnTabTypeId.setOnClickListener(v -> switchTab(2));
    }

    private void switchTab(int tab) {
        layoutClasses.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        layoutManual.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        layoutTypeId.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);

        // Define colors
        int activeBg = 0xFFCC0000; // Solid Red
        int inactiveBg = 0xFFF5F5F5; // Light Gray
        int activeText = 0xFFFFFFFF; // White
        int inactiveText = 0xFFCC0000; // Red text for inactive

        // Apply highlighting
        btnTabClasses.setBackgroundTintList(android.content.res.ColorStateList.valueOf(tab == 0 ? activeBg : inactiveBg));
        btnTabClasses.setTextColor(tab == 0 ? activeText : inactiveText);

        btnTabAttendance.setBackgroundTintList(android.content.res.ColorStateList.valueOf(tab == 1 ? activeBg : inactiveBg));
        btnTabAttendance.setTextColor(tab == 1 ? activeText : inactiveText);

        btnTabTypeId.setBackgroundTintList(android.content.res.ColorStateList.valueOf(tab == 2 ? activeBg : inactiveBg));
        btnTabTypeId.setTextColor(tab == 2 ? activeText : inactiveText);
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        navAttendance.setOnClickListener(v -> loadClassesOnline());
        navScanner.setOnClickListener(v -> startActivity(new Intent(this, ScannerActivity.class)));
        navReports.setOnClickListener(v -> startActivity(new Intent(this, ReportsActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }
}
