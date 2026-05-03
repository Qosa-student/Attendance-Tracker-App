package com.example.attendancetrackerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
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
    Button btnChangePic, btnSaveProfile, btnLogout, btnDeleteAccount;
    EditText etEditName, etEditPassword;
    TextView tvProfileName, tvProfileRole, tvInfoEmail;
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
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        etEditName = findViewById(R.id.etEditName);
        etEditPassword = findViewById(R.id.etEditPassword);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        tvInfoEmail = findViewById(R.id.tvInfoEmail);

        navHome = findViewById(R.id.navHome);
        navAttendance = findViewById(R.id.navAttendance);
        navScanner = findViewById(R.id.navScanner);
        navReports = findViewById(R.id.navReports);
        navProfile = findViewById(R.id.navProfile);

        SharedPreferences prefs = getSharedPreferences(
                "user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        String name = prefs.getString("name", "Teacher");
        String email = prefs.getString("email", "");

        tvProfileName.setText(name);
        tvProfileRole.setText("Teacher");
        tvInfoEmail.setText(email);
        etEditName.setText(name);

        loadProfilePic();

        navProfile.setOnClickListener(v -> {
            // already here, just refresh
            loadProfilePic();
        });

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
                loadProfilePic();
                Toast.makeText(this,
                        "Profile updated!", Toast.LENGTH_SHORT).show();
                etEditPassword.setText("");
            } else {
                Toast.makeText(this,
                        "Update failed.", Toast.LENGTH_SHORT).show();
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

        btnDeleteAccount.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Account")
                    .setMessage("Are you sure you want to permanently delete your account? This will remove all your classes, students, and records.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (db.deleteUser(userId)) {
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.clear();
                            editor.apply();
                            Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        setupNavigation();
    }

    private void loadProfilePic() {
        Cursor cursor = db.getUserById(userId);
        if (cursor.moveToFirst()) {
            String pic = cursor.getString(
                    cursor.getColumnIndexOrThrow("profile_pic"));
            String name = cursor.getString(
                    cursor.getColumnIndexOrThrow("name"));
            
            if (pic != null && !pic.isEmpty()) {
                byte[] bytes = Base64.decode(pic, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(
                        bytes, 0, bytes.length);
                imgProfilePic.setImageBitmap(bitmap);
                currentProfilePic = pic;
            } else {
                // Generate temporary avatar
                imgProfilePic.setImageBitmap(generateAvatar(name));
            }
        }
        cursor.close();
    }

    private Bitmap generateAvatar(String name) {
        int size = 200;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Background color based on name hash
        int[] colors = {0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 0xFF2196F3, 0xFF009688, 0xFF4CAF50, 0xFFFF9800, 0xFFFF5722};
        int color = colors[Math.abs(name.hashCode()) % colors.length];
        
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        
        // Text
        paint.setColor(Color.WHITE);
        paint.setTextSize(100);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        
        // Center text vertically
        Rect bounds = new Rect();
        paint.getTextBounds(initial, 0, 1, bounds);
        float y = (size / 2f) - bounds.centerY();
        
        canvas.drawText(initial, size / 2f, y, paint);
        
        return bitmap;
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
            Intent intent = new Intent(this, ScannerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
        navReports.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
        navProfile.setOnClickListener(v -> {
            // already on profile, just refresh
            loadProfilePic();
        });
    }
}