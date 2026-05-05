package com.example.attendancetrackerapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageStudentsActivity extends AppCompatActivity {

    EditText etStudentNumber, etStudentName;
    Button btnAddStudent;
    LinearLayout llStudentList;
    int classId;
    String className;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_students);

        classId = getIntent().getIntExtra("class_id", -1);
        className = getIntent().getStringExtra("class_name");

        etStudentNumber = findViewById(R.id.etStudentNumber);
        etStudentName = findViewById(R.id.etStudentName);
        btnAddStudent = findViewById(R.id.btnAddStudent);
        llStudentList = findViewById(R.id.llStudentList);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvManageTitle);
        tvTitle.setText(className);

        loadStudentsOnline();
        setupStudentNumberLookup();

        btnAddStudent.setOnClickListener(v -> {
            String number = etStudentNumber.getText().toString().trim();
            String name = etStudentName.getText().toString().trim();

            if (number.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            ApiService.create().addStudent(name, number, classId).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String result = response.body().string();
                            try {
                                org.json.JSONObject json = new org.json.JSONObject(result);
                                String status = json.getString("status");
                                String message = json.getString("message");

                                if (status.equals("success")) {
                                    Toast.makeText(ManageStudentsActivity.this, message, Toast.LENGTH_SHORT).show();
                                    etStudentNumber.setText("");
                                    etStudentName.setText("");
                                    loadStudentsOnline();
                                } else {
                                    Toast.makeText(ManageStudentsActivity.this, message, Toast.LENGTH_LONG).show();
                                }
                            } catch (org.json.JSONException e) {
                                // If parsing fails, show the raw result to help debug (e.g. PHP errors)
                                Toast.makeText(ManageStudentsActivity.this, "Error: " + result, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(ManageStudentsActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(ManageStudentsActivity.this, "Parsing error", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(ManageStudentsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupStudentNumberLookup() {
        etStudentNumber.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String number = s.toString().trim();
                // If it looks like a full ID (e.g. 2024-0001), try to find the name
                if (number.length() >= 5) {
                    findStudentNameGlobally(number);
                }
            }
        });
    }

    private void findStudentNameGlobally(String studentNumber) {
        android.content.SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        
        // Search all classes of this teacher to find the name for this ID
        ApiService.create().getClasses(userId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<ApiService.ClassModel>> call, Response<List<ApiService.ClassModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (ApiService.ClassModel cm : response.body()) {
                        ApiService.create().getStudents(cm.id).enqueue(new Callback<>() {
                            @Override
                            public void onResponse(Call<List<ApiService.StudentModel>> call, Response<List<ApiService.StudentModel>> sRes) {
                                if (sRes.isSuccessful() && sRes.body() != null) {
                                    for (ApiService.StudentModel sm : sRes.body()) {
                                        if (sm.student_number.equals(studentNumber)) {
                                            // If the name field is empty, auto-fill it!
                                            if (etStudentName.getText().toString().trim().isEmpty()) {
                                                etStudentName.setText(sm.name);
                                            }
                                            return;
                                        }
                                    }
                                }
                            }
                            @Override public void onFailure(Call<List<ApiService.StudentModel>> call, Throwable t) {}
                        });
                    }
                }
            }
            @Override public void onFailure(Call<List<ApiService.ClassModel>> call, Throwable t) {}
        });
    }

    private void loadStudentsOnline() {
        ApiService.create().getStudents(classId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<ApiService.StudentModel>> call, Response<List<ApiService.StudentModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    renderStudentList(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<ApiService.StudentModel>> call, Throwable t) {
                Toast.makeText(ManageStudentsActivity.this, "Failed to load students", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderStudentList(List<ApiService.StudentModel> students) {
        llStudentList.removeAllViews();
        if (students.isEmpty()) {
            TextView tvNone = new TextView(this);
            tvNone.setText("No students yet.");
            llStudentList.addView(tvNone);
            return;
        }

        for (ApiService.StudentModel sm : students) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(16, 16, 16, 16);
            card.setBackgroundColor(0xFFF5F5F5);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.setMargins(0, 0, 0, 8);
            card.setLayoutParams(cp);

            TextView tvName = new TextView(this);
            tvName.setText(sm.name);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvNum = new TextView(this);
            tvNum.setText(sm.student_number);

            Button btnRemove = new Button(this);
            btnRemove.setText("Remove");
            btnRemove.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFCC0000));
            btnRemove.setTextColor(0xFFFFFFFF);
            btnRemove.setOnClickListener(v -> {
                deleteStudentOnline(sm.id);
            });

            card.addView(tvName);
            card.addView(tvNum);
            card.addView(btnRemove);
            llStudentList.addView(card);
        }
    }

    private void deleteStudentOnline(int studentId) {
        ApiService.create().deleteStudent(studentId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ManageStudentsActivity.this, "Student removed", Toast.LENGTH_SHORT).show();
                    loadStudentsOnline();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }
}
