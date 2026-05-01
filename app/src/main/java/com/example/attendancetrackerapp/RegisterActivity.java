package com.example.attendancetrackerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.attendancetrackerapp.DataBase;

public class RegisterActivity extends AppCompatActivity {

    EditText etName, etEmail, etPassword, etConfirmPassword;
    Button btnRegister;
    TextView tvLogin;
    Spinner spinnerRole;
    DataBase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = new DataBase(this);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        spinnerRole = findViewById(R.id.spinnerRole);

        String[] roles = {"instructor", "admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                roles
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();
                String role = spinnerRole.getSelectedItem().toString();

                if (name.isEmpty() || email.isEmpty() ||
                        password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this,
                            "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this,
                            "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(RegisterActivity.this,
                            "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean success = db.registerUser(name, email, password, role);
                if (success) {
                    Toast.makeText(RegisterActivity.this,
                            "Account created! Please sign in.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "Email already exists. Try a different one.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}