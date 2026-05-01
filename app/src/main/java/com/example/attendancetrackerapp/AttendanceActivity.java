package com.example.attendancetrackerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import java.util.Locale;

public class AttendanceActivity extends AppCompatActivity {

    LinearLayout layoutClasses, layoutManual, layoutTypeId;
    LinearLayout llClassList, llStudentList, llMarkedList;
    LinearLayout llStudentFound, llNotFound;
    LinearLayout navHome, navAttendance, navScanner, navReports, navProfile;
    Button btnTabClasses, btnTabAttendance, btnTabTypeId;
    Button btnAddClass, btnSaveAttendance;
    Button btnMarkPresent, btnMarkAbsent, btnMarkLate;
    EditText etSubjectName, etSection, etSearch, etStudentId;
    TextView tvAttendanceDate, tvFoundName, tvFoundId;
    Spinner spinnerClass;
    DataBase db;
    int userId;
    int selectedClassId = -1;
    int foundStudentId = -1;
    String today;
    HashMap<Integer, String> attendanceMap = new HashMap<>();
    ArrayList<String> classNames = new ArrayList<>();
    ArrayList<Integer> classIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        db = new DataBase(this);
        today = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()).format(new Date());

        SharedPreferences prefs = getSharedPreferences(
                "user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        tvAttendanceDate = findViewById(R.id.tvAttendanceDate);
        tvAttendanceDate.setText(new SimpleDateFormat(
                "EEEE, MMMM d, yyyy",
                Locale.getDefault()).format(new Date()));

        layoutClasses = findViewById(R.id.layoutClasses);
        layoutManual = findViewById(R.id.layoutManual);
        layoutTypeId = findViewById(R.id.layoutTypeId);
        llClassList = findViewById(R.id.llClassList);
        llStudentList = findViewById(R.id.llStudentList);
        llMarkedList = findViewById(R.id.llMarkedList);
        llStudentFound = findViewById(R.id.llStudentFound);
        llNotFound = findViewById(R.id.llNotFound);
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
        tvFoundName = findViewById(R.id.tvFoundName);
        tvFoundId = findViewById(R.id.tvFoundId);
        spinnerClass = findViewById(R.id.spinnerClass);

        navHome = findViewById(R.id.navHome);
        navAttendance = findViewById(R.id.navAttendance);
        navScanner = findViewById(R.id.navScanner);
        navReports = findViewById(R.id.navReports);
        navProfile = findViewById(R.id.navProfile);

        loadClasses();
        setupTabs();
        setupClassSpinner();
        setupTypeId();
        setupNavigation();

        btnAddClass.setOnClickListener(v -> {
            String subject = etSubjectName.getText().toString().trim();
            String section = etSection.getText().toString().trim();
            if (subject.isEmpty() || section.isEmpty()) {
                Toast.makeText(this,
                        "Please fill in subject and section",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            boolean success = db.addClass(subject, section, userId);
            if (success) {
                Toast.makeText(this,
                        "Class added!", Toast.LENGTH_SHORT).show();
                etSubjectName.setText("");
                etSection.setText("");
                loadClasses();
                setupClassSpinner();
            } else {
                Toast.makeText(this,
                        "Failed to add class.", Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveAttendance.setOnClickListener(v -> {
            if (attendanceMap.isEmpty()) {
                Toast.makeText(this,
                        "No attendance marked yet.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            for (HashMap.Entry<Integer, String> entry :
                    attendanceMap.entrySet()) {
                db.markAttendance(entry.getKey(), today, entry.getValue());
            }
            Toast.makeText(this,
                    "Attendance saved!", Toast.LENGTH_SHORT).show();
            attendanceMap.clear();
            loadStudentList("");
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(
                    CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(
                    CharSequence s, int a, int b, int c) {
                loadStudentList(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadClasses() {
        llClassList.removeAllViews();
        Cursor cursor = db.getClassesByInstructor(userId);

        if (cursor.getCount() == 0) {
            TextView tvNone = new TextView(this);
            tvNone.setText("No classes yet. Add your first class above.");
            tvNone.setTextSize(13f);
            tvNone.setTextColor(0xFF555555);
            llClassList.addView(tvNone);
            cursor.close();
            return;
        }

        if (cursor.moveToFirst()) {
            do {
                int classId = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id"));
                String subject = cursor.getString(
                        cursor.getColumnIndexOrThrow("subject_name"));
                String section = cursor.getString(
                        cursor.getColumnIndexOrThrow("section"));

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

                TextView tvSubject = new TextView(this);
                tvSubject.setText(subject);
                tvSubject.setTextSize(14f);
                tvSubject.setTextColor(0xFF000000);
                tvSubject.setTypeface(null,
                        android.graphics.Typeface.BOLD);

                TextView tvSection = new TextView(this);
                tvSection.setText("Section: " + section);
                tvSection.setTextSize(12f);
                tvSection.setTextColor(0xFF555555);

                info.addView(tvSubject);
                info.addView(tvSection);

                Button btnStudents = new Button(this);
                btnStudents.setText("Students");
                btnStudents.setTextSize(11f);
                btnStudents.setTextColor(0xFFFFFFFF);
                btnStudents.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF1A3A8F));
                LinearLayout.LayoutParams sp =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                sp.setMargins(8, 0, 0, 0);
                btnStudents.setLayoutParams(sp);
                btnStudents.setOnClickListener(v -> {
                    Intent intent = new Intent(this,
                            ManageStudentsActivity.class);
                    intent.putExtra("class_id", classId);
                    intent.putExtra("class_name",
                            subject + " - " + section);
                    startActivity(intent);
                });

                Button btnDelete = new Button(this);
                btnDelete.setText("Delete");
                btnDelete.setTextSize(11f);
                btnDelete.setTextColor(0xFFFFFFFF);
                btnDelete.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFCC0000));
                LinearLayout.LayoutParams dp =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                dp.setMargins(8, 0, 0, 0);
                btnDelete.setLayoutParams(dp);
                btnDelete.setOnClickListener(v -> {
                    db.deleteClass(classId);
                    Toast.makeText(this,
                            "Class deleted.", Toast.LENGTH_SHORT).show();
                    loadClasses();
                    setupClassSpinner();
                });

                topRow.addView(info);
                topRow.addView(btnStudents);
                topRow.addView(btnDelete);
                card.addView(topRow);
                llClassList.addView(card);

            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void setupClassSpinner() {
        classNames.clear();
        classIds.clear();
        classNames.add("-- Select Class --");
        classIds.add(-1);

        Cursor cursor = db.getClassesByInstructor(userId);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String subject = cursor.getString(
                        cursor.getColumnIndexOrThrow("subject_name"));
                String section = cursor.getString(
                        cursor.getColumnIndexOrThrow("section"));
                classIds.add(id);
                classNames.add(subject + " - " + section);
            } while (cursor.moveToNext());
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, classNames);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerClass.setAdapter(adapter);

        spinnerClass.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view, int position, long id) {
                        selectedClassId = classIds.get(position);
                        loadStudentList("");
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
    }

    private void loadStudentList(String query) {
        llStudentList.removeAllViews();
        if (selectedClassId == -1) return;

        Cursor cursor = db.getStudentsByClass(selectedClassId);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name"));
                String number = cursor.getString(
                        cursor.getColumnIndexOrThrow("student_number"));
                int sid = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id"));

                if (!query.isEmpty() &&
                        !name.toLowerCase().contains(query.toLowerCase()) &&
                        !number.contains(query)) continue;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 12, 0, 12);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                LinearLayout info = new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams ip =
                        new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                info.setLayoutParams(ip);

                TextView tvName = new TextView(this);
                tvName.setText(name);
                tvName.setTextSize(13f);
                tvName.setTextColor(0xFF000000);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);

                TextView tvNum = new TextView(this);
                tvNum.setText(number);
                tvNum.setTextSize(11f);
                tvNum.setTextColor(0xFF555555);

                info.addView(tvName);
                info.addView(tvNum);

                LinearLayout buttons = new LinearLayout(this);
                String[] labels = {"P", "A", "L"};
                String[] statuses = {"present", "absent", "late"};
                int[] colors = {0xFF16A34A, 0xFFCC0000, 0xFFD97706};

                for (int i = 0; i < 3; i++) {
                    final String status = statuses[i];
                    Button btn = new Button(this);
                    btn.setText(labels[i]);
                    btn.setTextColor(0xFFFFFFFF);
                    btn.setTextSize(11f);
                    LinearLayout.LayoutParams bp =
                            new LinearLayout.LayoutParams(80, 80);
                    bp.setMargins(4, 0, 0, 0);
                    btn.setLayoutParams(bp);
                    btn.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    colors[i]));
                    btn.setAlpha(attendanceMap.containsKey(sid) &&
                            attendanceMap.get(sid).equals(status) ? 1f : 0.4f);
                    btn.setOnClickListener(v -> {
                        attendanceMap.put(sid, status);
                        loadStudentList(etSearch.getText().toString());
                    });
                    buttons.addView(btn);
                }

                row.addView(info);
                row.addView(buttons);

                View divider = new View(this);
                divider.setBackgroundColor(0xFFEEEEEE);
                LinearLayout.LayoutParams divParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divider.setLayoutParams(divParams);

                llStudentList.addView(row);
                llStudentList.addView(divider);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void setupTypeId() {
        etStudentId.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(
                    CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(
                    CharSequence s, int a, int b, int c) {
                String id = s.toString().trim();
                if (id.length() >= 9) {
                    Cursor cursor = db.findStudentByNumber(id);
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        foundStudentId = cursor.getInt(
                                cursor.getColumnIndexOrThrow("id"));
                        String name = cursor.getString(
                                cursor.getColumnIndexOrThrow("name"));
                        String num = cursor.getString(
                                cursor.getColumnIndexOrThrow("student_number"));
                        tvFoundName.setText(name);
                        tvFoundId.setText(num + " · Batch " +
                                num.substring(0, 4));
                        llStudentFound.setVisibility(View.VISIBLE);
                        llNotFound.setVisibility(View.GONE);
                    } else {
                        foundStudentId = -1;
                        llStudentFound.setVisibility(View.GONE);
                        llNotFound.setVisibility(View.VISIBLE);
                    }
                    cursor.close();
                } else {
                    llStudentFound.setVisibility(View.GONE);
                    llNotFound.setVisibility(View.GONE);
                    foundStudentId = -1;
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnMarkPresent.setOnClickListener(v -> markFromTypeId("present"));
        btnMarkAbsent.setOnClickListener(v -> markFromTypeId("absent"));
        btnMarkLate.setOnClickListener(v -> markFromTypeId("late"));
    }

    private void markFromTypeId(String status) {
        if (foundStudentId == -1) {
            Toast.makeText(this,
                    "No valid student found.", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean success = db.markAttendance(foundStudentId, today, status);
        if (success) {
            Toast.makeText(this,
                    "Marked as " + status + "!", Toast.LENGTH_SHORT).show();
            etStudentId.setText("");
            llStudentFound.setVisibility(View.GONE);
            foundStudentId = -1;
            loadMarkedList();
        } else {
            Toast.makeText(this,
                    "Already marked today.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMarkedList() {
        llMarkedList.removeAllViews();
        Cursor cursor = db.getAttendanceByDate(today);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name"));
                String number = cursor.getString(
                        cursor.getColumnIndexOrThrow("student_number"));
                String status = cursor.getString(
                        cursor.getColumnIndexOrThrow("status"));

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 8, 0, 8);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                TextView tvInfo = new TextView(this);
                tvInfo.setText(name + " · " + number);
                tvInfo.setTextSize(13f);
                tvInfo.setTextColor(0xFF000000);
                LinearLayout.LayoutParams p =
                        new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tvInfo.setLayoutParams(p);

                TextView tvStatus = new TextView(this);
                tvStatus.setText(status.toUpperCase());
                tvStatus.setTextSize(11f);
                tvStatus.setPadding(12, 4, 12, 4);
                if (status.equals("present")) {
                    tvStatus.setBackgroundColor(0xFFDCFCE7);
                    tvStatus.setTextColor(0xFF15803D);
                } else if (status.equals("absent")) {
                    tvStatus.setBackgroundColor(0xFFFEE2E2);
                    tvStatus.setTextColor(0xFFB91C1C);
                } else {
                    tvStatus.setBackgroundColor(0xFFFEF9C3);
                    tvStatus.setTextColor(0xFFA16207);
                }

                row.addView(tvInfo);
                row.addView(tvStatus);
                llMarkedList.addView(row);
            } while (cursor.moveToNext());
        }
        cursor.close();
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

        int red = 0xFFCC0000;
        int gray = 0xFFF5F5F5;

        btnTabClasses.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        tab == 0 ? red : gray));
        btnTabClasses.setTextColor(tab == 0 ? 0xFFFFFFFF : 0xFFCC0000);

        btnTabAttendance.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        tab == 1 ? red : gray));
        btnTabAttendance.setTextColor(tab == 1 ? 0xFFFFFFFF : 0xFFCC0000);

        btnTabTypeId.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        tab == 2 ? red : gray));
        btnTabTypeId.setTextColor(tab == 2 ? 0xFFFFFFFF : 0xFFCC0000);
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v ->
                startActivity(new Intent(this, HomeActivity.class)));
        navScanner.setOnClickListener(v ->
                startActivity(new Intent(this, ScannerActivity.class)));
        navReports.setOnClickListener(v ->
                startActivity(new Intent(this, ReportsActivity.class)));
        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }
}