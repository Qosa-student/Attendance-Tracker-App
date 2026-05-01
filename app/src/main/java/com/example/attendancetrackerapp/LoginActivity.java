package com.example.attendancetrackerapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvRegister;
    DataBase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new DataBase(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this,
                            "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                Cursor cursor = db.loginUser(email, password);
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String savedEmail = cursor.getString(
                            cursor.getColumnIndexOrThrow("email"));
                    String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));

                    SharedPreferences.Editor editor = getSharedPreferences(
                            "user_session", MODE_PRIVATE).edit();
                    editor.putInt("user_id", id);
                    editor.putString("name", name);
                    editor.putString("email", savedEmail);
                    editor.putString("role", role);
                    editor.apply();

                    Toast.makeText(LoginActivity.this,
                            "Login successful!", Toast.LENGTH_SHORT).show();

                    if (role.equals("admin")) {
                        startActivity(new Intent(
                                LoginActivity.this, AdminDashboardActivity.class));
                    } else {
                        startActivity(new Intent(
                                LoginActivity.this, HomeActivity.class));
                    }
                    finish();
                }
                cursor.close();
            }
        });

        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}