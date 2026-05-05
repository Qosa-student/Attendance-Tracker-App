package com.example.attendancetrackerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.imageview.ShapeableImageView;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    ShapeableImageView imgProfilePic;
    Button btnSaveProfile, btnLogout, btnDeleteAccount, btnChangePic;
    EditText etEditName, etEditPassword;
    TextView tvProfileName, tvProfileRole, tvInfoEmail;
    LinearLayout navHome, navAttendance, navScanner, navReports, navProfile;
    int userId;
    private static final int PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        imgProfilePic = findViewById(R.id.imgProfilePic);
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

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        String name = prefs.getString("name", "Teacher");
        String email = prefs.getString("email", "");

        // Load profile picture from persistent storage based on userId
        SharedPreferences persistentPrefs = getSharedPreferences("persistent_profile_data", MODE_PRIVATE);
        String profileUri = persistentPrefs.getString("profile_uri_" + userId, null);

        tvProfileName.setText(name);
        tvProfileRole.setText("Teacher");
        tvInfoEmail.setText(email);
        etEditName.setText(name);

        btnChangePic = findViewById(R.id.btnChangePic);

        if (profileUri != null) {
            try {
                imgProfilePic.setImageURI(android.net.Uri.parse(profileUri));
            } catch (Exception e) {
                imgProfilePic.setImageBitmap(generateAvatar(name));
            }
        } else {
            imgProfilePic.setImageBitmap(generateAvatar(name));
        }

        btnChangePic.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });

        btnSaveProfile.setOnClickListener(v -> {
            String newName = etEditName.getText().toString().trim();
            String newPass = etEditPassword.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            updateProfileOnline(newName, newPass, prefs);
        });

        btnLogout.setOnClickListener(v -> logoutUser());

        btnDeleteAccount.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Account")
                    .setMessage("This will remove all your data permanently.")
                    .setPositiveButton("Delete", (dialog, which) -> deleteAccountOnline())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        setupNavigation();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            android.net.Uri selectedImage = data.getData();
            getContentResolver().takePersistableUriPermission(selectedImage, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            imgProfilePic.setImageURI(selectedImage);

            // Save to persistent storage linked to user ID
            SharedPreferences.Editor editor = getSharedPreferences("persistent_profile_data", MODE_PRIVATE).edit();
            editor.putString("profile_uri_" + userId, selectedImage.toString());
            editor.apply();
            Toast.makeText(this, "Photo updated!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProfileOnline(String name, String pass, SharedPreferences prefs) {
        SharedPreferences persistentPrefs = getSharedPreferences("persistent_profile_data", MODE_PRIVATE);
        String profilePic = persistentPrefs.getString("profile_uri_" + userId, "");
        ApiService.create().updateProfile(userId, name, pass, profilePic).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("name", name);
                    editor.apply();
                    tvProfileName.setText(name);

                    // Only regenerate avatar if no custom image is set
                    SharedPreferences persistentPrefs = getSharedPreferences("persistent_profile_data", MODE_PRIVATE);
                    if (persistentPrefs.getString("profile_uri_" + userId, null) == null) {
                        imgProfilePic.setImageBitmap(generateAvatar(name));
                    }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void deleteAccountOnline() {
        ApiService.create().deleteAccount(userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Permanently clear profile pic on account deletion
                    SharedPreferences.Editor editor = getSharedPreferences("persistent_profile_data", MODE_PRIVATE).edit();
                    editor.remove("profile_uri_" + userId);
                    editor.apply();

                    Toast.makeText(ProfileActivity.this, "Account deleted", Toast.LENGTH_SHORT).show();
                    logoutUser();
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void logoutUser() {
        SharedPreferences.Editor editor = getSharedPreferences("user_session", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private Bitmap generateAvatar(String name) {
        int size = 200;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        int[] colors = {0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 0xFF2196F3, 0xFF009688, 0xFF4CAF50, 0xFFFF9800, 0xFFFF5722};
        int color = colors[Math.abs(name.hashCode()) % colors.length];
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(100);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        Rect bounds = new Rect();
        paint.getTextBounds(initial, 0, 1, bounds);
        canvas.drawText(initial, size / 2f, (size / 2f) - bounds.centerY(), paint);
        return bitmap;
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        navAttendance.setOnClickListener(v -> startActivity(new Intent(this, AttendanceActivity.class)));
        navScanner.setOnClickListener(v -> startActivity(new Intent(this, SeatingActivity.class)));
        navReports.setOnClickListener(v -> startActivity(new Intent(this, ReportsActivity.class)));
        navProfile.setOnClickListener(v -> {});
    }
}