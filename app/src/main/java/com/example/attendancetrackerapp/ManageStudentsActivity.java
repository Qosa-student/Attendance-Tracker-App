package com.example.attendancetrackerapp;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ManageStudentsActivity extends AppCompatActivity {

    EditText etStudentNumber, etStudentName;
    Button btnAddStudent;
    LinearLayout llStudentList;
    DataBase db;
    int classId;
    String className;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_students);

        db = new DataBase(this);
        classId = getIntent().getIntExtra("class_id", -1);
        className = getIntent().getStringExtra("class_name");

        etStudentNumber = findViewById(R.id.etStudentNumber);
        etStudentName = findViewById(R.id.etStudentName);
        btnAddStudent = findViewById(R.id.btnAddStudent);
        llStudentList = findViewById(R.id.llStudentList);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvManageTitle);
        tvTitle.setText(className);

        loadStudents();

        btnAddStudent.setOnClickListener(v -> {
            String number = etStudentNumber.getText().toString().trim();
            String name = etStudentName.getText().toString().trim();

            if (number.isEmpty() || name.isEmpty()) {
                Toast.makeText(this,
                        "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!number.matches("\\d{4}-\\d{4}")) {
                Toast.makeText(this,
                        "ID format must be YYYY-XXXX (e.g. 2024-0001)",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success = db.addStudentToClass(number, name, classId);
            if (success) {
                Toast.makeText(this,
                        "Student added!", Toast.LENGTH_SHORT).show();
                etStudentNumber.setText("");
                etStudentName.setText("");
                loadStudents();
            } else {
                Toast.makeText(this,
                        "This ID number has already been used, please re-enter another ID number",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadStudents() {
        llStudentList.removeAllViews();
        Cursor cursor = db.getStudentsByClass(classId);

        if (cursor.getCount() == 0) {
            TextView tvNone = new TextView(this);
            tvNone.setText("No students in this class yet.");
            tvNone.setTextSize(13f);
            tvNone.setTextColor(0xFF555555);
            llStudentList.addView(tvNone);
            cursor.close();
            return;
        }

        if (cursor.moveToFirst()) {
            do {
                int studentId = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name"));
                String number = cursor.getString(
                        cursor.getColumnIndexOrThrow("student_number"));
                String dateAdded = cursor.getString(
                        cursor.getColumnIndexOrThrow("date_added"));

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(0, 10, 0, 10);
                LinearLayout.LayoutParams rowParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 0, 0, 4);
                row.setLayoutParams(rowParams);

                LinearLayout info = new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoParams =
                        new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                info.setLayoutParams(infoParams);

                TextView tvName = new TextView(this);
                tvName.setText(name);
                tvName.setTextSize(14f);
                tvName.setTextColor(0xFF000000);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);

                TextView tvNumber = new TextView(this);
                tvNumber.setText(number);
                tvNumber.setTextSize(12f);
                tvNumber.setTextColor(0xFF555555);

                TextView tvDate = new TextView(this);
                if (dateAdded != null) {
                    tvDate.setText("Added: " + dateAdded);
                } else {
                    tvDate.setText("Added: Prior to tracking");
                }
                tvDate.setTextSize(10f);
                tvDate.setTextColor(0xFF888888);

                info.addView(tvName);
                info.addView(tvNumber);
                info.addView(tvDate);

                Button btnRemove = new Button(this);
                btnRemove.setText("Remove");
                btnRemove.setTextSize(11f);
                btnRemove.setTextColor(0xFFFFFFFF);
                btnRemove.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFCC0000));
                btnRemove.setOnClickListener(v -> {
                    db.deleteStudentFromClass(studentId);
                    loadStudents();
                    Toast.makeText(this,
                            "Student removed.", Toast.LENGTH_SHORT).show();
                });

                row.addView(info);
                row.addView(btnRemove);

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
}