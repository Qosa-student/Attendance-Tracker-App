package com.example.attendancetrackerapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import androidx.activity.result.ActivityResultLauncher;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScannerActivity extends AppCompatActivity {

    Button btnScan, btnScanPresent, btnScanAbsent, btnScanLate;
    LinearLayout llScanResult, llScanFound, llScanError, llScannedList;
    LinearLayout navHome, navAttendance, navScanner, navReports, navProfile;
    TextView tvScanName, tvScanId;
    DataBase db;
    int foundStudentId = -1;
    String foundClassName = "";
    String today;
    int userId;

    ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    String scannedId = result.getContents().trim();
                    processScannedId(scannedId);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        db = new DataBase(this);
        
        android.content.SharedPreferences prefs = getSharedPreferences(
                "user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        
        today = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()).format(new Date());

        btnScan = findViewById(R.id.btnScan);
        btnScanPresent = findViewById(R.id.btnScanPresent);
        btnScanAbsent = findViewById(R.id.btnScanAbsent);
        btnScanLate = findViewById(R.id.btnScanLate);
        llScanResult = findViewById(R.id.llScanResult);
        llScanFound = findViewById(R.id.llScanFound);
        llScanError = findViewById(R.id.llScanError);
        llScannedList = findViewById(R.id.llScannedList);
        tvScanName = findViewById(R.id.tvScanName);
        tvScanId = findViewById(R.id.tvScanId);

        navHome = findViewById(R.id.navHome);
        navAttendance = findViewById(R.id.navAttendance);
        navScanner = findViewById(R.id.navScanner);
        navReports = findViewById(R.id.navReports);
        navProfile = findViewById(R.id.navProfile);

        loadScannedList();

        btnScan.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan student QR ID card");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setCaptureActivity(null);
            scanLauncher.launch(options);
        });

        btnScanPresent.setOnClickListener(v -> markScanned("present"));
        btnScanAbsent.setOnClickListener(v -> markScanned("absent"));
        btnScanLate.setOnClickListener(v -> markScanned("late"));

        setupNavigation();
    }

    private void processScannedId(String scannedId) {
        llScanResult.setVisibility(View.VISIBLE);
        Cursor cursor = db.findStudentByNumber(scannedId, userId);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            foundStudentId = cursor.getInt(
                    cursor.getColumnIndexOrThrow("id"));
            String name = cursor.getString(
                    cursor.getColumnIndexOrThrow("name"));
            String studentNumber = cursor.getString(
                    cursor.getColumnIndexOrThrow("student_number"));
            String subject = cursor.getString(
                    cursor.getColumnIndexOrThrow("subject_name"));
            String section = cursor.getString(
                    cursor.getColumnIndexOrThrow("section"));
            foundClassName = subject + " - " + section;
            String batch = studentNumber.substring(0, 4);

            tvScanName.setText(name);
            tvScanId.setText(studentNumber + " · Batch " + batch);
            llScanFound.setVisibility(View.VISIBLE);
            llScanError.setVisibility(View.GONE);
        } else {
            foundStudentId = -1;
            foundClassName = "";
            llScanFound.setVisibility(View.GONE);
            llScanError.setVisibility(View.VISIBLE);
        }
        cursor.close();
    }

    private void markScanned(String status) {
        if (foundStudentId == -1) {
            Toast.makeText(this,
                    "No valid student scanned.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.content.SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String tName = prefs.getString("name", "Teacher");
        String sName = tvScanName.getText().toString();

        boolean success = db.markAttendance(foundStudentId, sName, foundClassName, userId, tName, today, status);
        if (success) {
            Toast.makeText(this,
                    "Marked as " + status + "!", Toast.LENGTH_SHORT).show();
            llScanResult.setVisibility(View.GONE);
            llScanFound.setVisibility(View.GONE);
            llScanError.setVisibility(View.GONE);
            foundStudentId = -1;
            foundClassName = "";
            loadScannedList();
        } else {
            Toast.makeText(this,
                    "Failed to mark attendance.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadScannedList() {
        llScannedList.removeAllViews();
        Cursor cursor = db.getAttendanceByDate(today, userId);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name"));
                String studentNumber = cursor.getString(
                        cursor.getColumnIndexOrThrow("student_number"));
                String status = cursor.getString(
                        cursor.getColumnIndexOrThrow("status"));

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 8, 0, 8);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                TextView tvInfo = new TextView(this);
                tvInfo.setText(name + " · " + studentNumber);
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
                llScannedList.addView(row);

            } while (cursor.moveToNext());
        }
        cursor.close();
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
            // already on scanner, just reset the scan result
            llScanResult.setVisibility(View.GONE);
            llScanFound.setVisibility(View.GONE);
            llScanError.setVisibility(View.GONE);
            foundStudentId = -1;
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