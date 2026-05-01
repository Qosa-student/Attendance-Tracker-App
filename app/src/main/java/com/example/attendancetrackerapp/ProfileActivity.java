package com.example.attendancetrackerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {

    ImageView imgProfilePic;
    Button btnChangePic, btnSaveProfile, btnAddClass, btnLogout;
    EditText etEditName, etEditPassword, etSubjectName, etClassSection;
    TextView tvProfileName, tvProfileRole, tvInfoEmail;
    LinearLayout llClassList;
    LinearLayout navHome, navAttendance, navScanner, navReports, navProfile;
    DataBase db;
    int userId;
    String currentProfilePic = null;

    ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(), uri -> {
                        if (uri != null) {
                            try {
                                Bitmap bitmap = MediaStore.Images.Media
                                        .getBitmap(getContentResolver(), uri);
                                Bitmap scaled = Bitmap.createScaledBitmap(
                                        bitmap, 200, 200, true);
                                imgProfilePic.setImageBitmap(scaled);
                                ByteArrayOutputStream baos =
                                        new ByteArrayOutputStream();
                                scaled.compress(
                                        Bitmap.CompressFormat.JPEG, 80, baos);
                                currentProfilePic = Base64.encodeToString(
                                        baos.toByteArray(), Base64.DEFAULT);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = new DataBase(this);

        imgProfilePic = findViewById(R.id.imgProfilePic);
        btnChangePic = findViewById(R.id.btnChangePic);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnAddClass = findViewById(R.id.btnAddClass);
        btnLogout = findViewById(R.id.btnLogout);
        etEditName = findViewById(R.id.etEditName);
        etEditPassword = findViewById(R.id.etEditPassword);
        etSubjectName = findViewById(R.id.etSubjectName);
        etClassSection = findViewById(R.id.etClassSection);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        tvInfoEmail = findViewById(R.id.tvInfoEmail);
        llClassList = findViewById(R.id.llClassList);

        navHome = findViewById(R.id.navHome);
        navAttendance = findViewById(R.id.navAttendance);
        navScanner = findViewById(R.id.navScanner);
        navReports = findViewById(R.id.navReports);
        navProfile = findViewById(R.id.navProfile);

        SharedPreferences prefs = getSharedPreferences(
                "user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        String name = prefs.getString("name", "Instructor");
        String email = prefs.getString("email", "");
        String role = prefs.getString("role", "instructor");

        tvProfileName.setText(name);
        tvProfileRole.setText(role.substring(0, 1).toUpperCase()
                + role.substring(1));
        tvInfoEmail.setText(email);
        etEditName.setText(name);

        loadProfilePic();
        loadClasses();

        btnChangePic.setOnClickListener(v ->
                imagePickerLauncher.launch("image/*"));

        btnSaveProfile.setOnClickListener(v -> {
            String newName = etEditName.getText().toString().trim();
            String newPassword = etEditPassword.getText().toString().trim();

            if (newName.isEmpty()) {
                Toast.makeText(this,
                        "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success = db.updateUserProfile(
                    userId, newName, newPassword, currentProfilePic);
            if (success) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("name", newName);
                editor.apply();
                tvProfileName.setText(newName);
                Toast.makeText(this,
                        "Profile updated!", Toast.LENGTH_SHORT).show();
                etEditPassword.setText("");
            } else {
                Toast.makeText(this,
                        "Update failed.", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddClass.setOnClickListener(v -> {
            String subject = etSubjectName.getText().toString().trim();
            String section = etClassSection.getText().toString().trim();

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
                etClassSection.setText("");
                loadClasses();
            } else {
                Toast.makeText(this,
                        "Failed to add class.", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        setupNavigation();
    }

    private void loadProfilePic() {
        Cursor cursor = db.getUserById(userId);
        if (cursor.moveToFirst()) {
            String pic = cursor.getString(
                    cursor.getColumnIndexOrThrow("profile_pic"));
            if (pic != null && !pic.isEmpty()) {
                byte[] bytes = Base64.decode(pic, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(
                        bytes, 0, bytes.length);
                imgProfilePic.setImageBitmap(bitmap);
                currentProfilePic = pic;
            }
        }
        cursor.close();
    }

    private void loadClasses() {
        llClassList.removeAllViews();
        Cursor cursor = db.getClassesByInstructor(userId);

        if (cursor.getCount() == 0) {
            TextView tvNone = new TextView(this);
            tvNone.setText("No classes added yet.");
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
                LinearLayout.LayoutParams cardParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 0, 0, 8);
                card.setLayoutParams(cardParams);

                LinearLayout topRow = new LinearLayout(this);
                topRow.setOrientation(LinearLayout.HORIZONTAL);
                topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

                LinearLayout info = new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoParams =
                        new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                info.setLayoutParams(infoParams);

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

                Button btnDelete = new Button(this);
                btnDelete.setText("Remove");
                btnDelete.setTextSize(11f);
                btnDelete.setTextColor(0xFFFFFFFF);
                btnDelete.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFCC0000));
                LinearLayout.LayoutParams delParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                btnDelete.setLayoutParams(delParams);
                btnDelete.setOnClickListener(v -> {
                    db.deleteClass(classId);
                    loadClasses();
                    Toast.makeText(this,
                            "Class removed.", Toast.LENGTH_SHORT).show();
                });

                Button btnManageStudents = new Button(this);
                btnManageStudents.setText("Students");
                btnManageStudents.setTextSize(11f);
                btnManageStudents.setTextColor(0xFFFFFFFF);
                btnManageStudents.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF1A3A8F));
                LinearLayout.LayoutParams msParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                msParams.setMargins(8, 0, 0, 0);
                btnManageStudents.setLayoutParams(msParams);
                btnManageStudents.setOnClickListener(v -> {
                    Intent intent = new Intent(this,
                            ManageStudentsActivity.class);
                    intent.putExtra("class_id", classId);
                    intent.putExtra("class_name",
                            subject + " - " + section);
                    startActivity(intent);
                });

                topRow.addView(info);
                topRow.addView(btnDelete);
                topRow.addView(btnManageStudents);
                card.addView(topRow);
                llClassList.addView(card);

            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v ->
                startActivity(new Intent(this, HomeActivity.class)));
        navAttendance.setOnClickListener(v ->
                startActivity(new Intent(this, AttendanceActivity.class)));
        navScanner.setOnClickListener(v ->
                startActivity(new Intent(this, ScannerActivity.class)));
        navReports.setOnClickListener(v ->
                startActivity(new Intent(this, ReportsActivity.class)));
    }
}