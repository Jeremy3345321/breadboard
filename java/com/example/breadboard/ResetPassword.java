package com.example.breadboard;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ResetPassword extends AppCompatActivity {

    private TextInputEditText usernameEditText, newPasswordEditText, confirmPasswordEditText;
    private TextInputLayout usernameLayout, newPasswordLayout, confirmPasswordLayout;
    private MaterialButton resetPasswordButton, backToLoginButton;
    private LinearLayout passwordStrengthLayout;
    private View strengthBar1, strengthBar2, strengthBar3;
    private TextView strengthText;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupEventListeners();
    }

    private void initializeViews() {
        // Initialize database helper
        dbHelper = new DBHelper(this);

        // Initialize text inputs
        usernameEditText = findViewById(R.id.username_edittext);
        newPasswordEditText = findViewById(R.id.new_password_edittext);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edittext);

        // Initialize input layouts
        usernameLayout = findViewById(R.id.username_layout);
        newPasswordLayout = findViewById(R.id.new_password_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);

        // Initialize buttons
        resetPasswordButton = findViewById(R.id.btn_reset_password);
        backToLoginButton = findViewById(R.id.btn_back_to_login);

        // Initialize password strength indicator
        passwordStrengthLayout = findViewById(R.id.password_strength_layout);
        strengthBar1 = findViewById(R.id.strength_bar_1);
        strengthBar2 = findViewById(R.id.strength_bar_2);
        strengthBar3 = findViewById(R.id.strength_bar_3);
        strengthText = findViewById(R.id.strength_text);
    }

    private void setupEventListeners() {
        // Username text watcher
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateUsername();
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // New password text watcher
        newPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
                validateNewPassword();
                validatePasswordMatch();
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Confirm password text watcher
        confirmPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePasswordMatch();
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Reset password button click listener
        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });

        // Back to login button click listener
        backToLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResetPassword.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private boolean validateUsername() {
        String username = usernameEditText.getText().toString().trim();

        if (username.isEmpty()) {
            usernameLayout.setError("Username is required");
            return false;
        }

        if (username.length() < 3) {
            usernameLayout.setError("Username must be at least 3 characters");
            return false;
        }

        // Check if username exists in database
        if (!dbHelper.checkusername(username)) {
            usernameLayout.setError("Username not found");
            return false;
        }

        usernameLayout.setError(null);
        usernameLayout.setHelperText("Username found ✓");
        return true;
    }

    private boolean validateNewPassword() {
        String password = newPasswordEditText.getText().toString();

        if (password.isEmpty()) {
            newPasswordLayout.setError("Password is required");
            return false;
        }

        if (password.length() < 8) {
            newPasswordLayout.setError("Password must be at least 8 characters");
            return false;
        }

        if (!password.matches(".*[A-Z].*")) {
            newPasswordLayout.setError("Password must contain at least one uppercase letter");
            return false;
        }

        if (!password.matches(".*[0-9].*")) {
            newPasswordLayout.setError("Password must contain at least one number");
            return false;
        }

        newPasswordLayout.setError(null);
        newPasswordLayout.setHelperText("Password strength: " + getPasswordStrengthText(password));
        return true;
    }

    private boolean validatePasswordMatch() {
        String password = newPasswordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.setError("Please confirm your password");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
            return false;
        }

        confirmPasswordLayout.setError(null);
        confirmPasswordLayout.setHelperText("Passwords match ✓");
        return true;
    }

    private void validateForm() {
        boolean isUsernameValid = validateUsername();
        boolean isNewPasswordValid = validateNewPassword();
        boolean isPasswordMatchValid = validatePasswordMatch();

        boolean isFormValid = isUsernameValid && isNewPasswordValid && isPasswordMatchValid;

        resetPasswordButton.setEnabled(isFormValid);
        resetPasswordButton.setAlpha(isFormValid ? 1.0f : 0.6f);
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrengthLayout.setVisibility(View.GONE);
            return;
        }

        passwordStrengthLayout.setVisibility(View.VISIBLE);

        int strength = calculatePasswordStrength(password);

        // Reset all bars
        strengthBar1.setBackgroundColor(getColor(android.R.color.darker_gray));
        strengthBar2.setBackgroundColor(getColor(android.R.color.darker_gray));
        strengthBar3.setBackgroundColor(getColor(android.R.color.darker_gray));

        switch (strength) {
            case 1:
                strengthBar1.setBackgroundColor(getColor(android.R.color.holo_red_light));
                strengthText.setText("Weak");
                strengthText.setTextColor(getColor(android.R.color.holo_red_light));
                break;
            case 2:
                strengthBar1.setBackgroundColor(getColor(android.R.color.holo_orange_light));
                strengthBar2.setBackgroundColor(getColor(android.R.color.holo_orange_light));
                strengthText.setText("Medium");
                strengthText.setTextColor(getColor(android.R.color.holo_orange_light));
                break;
            case 3:
                strengthBar1.setBackgroundColor(getColor(android.R.color.holo_green_light));
                strengthBar2.setBackgroundColor(getColor(android.R.color.holo_green_light));
                strengthBar3.setBackgroundColor(getColor(android.R.color.holo_green_light));
                strengthText.setText("Strong");
                strengthText.setTextColor(getColor(android.R.color.holo_green_light));
                break;
        }
    }

    private int calculatePasswordStrength(String password) {
        int score = 0;

        if (password.length() >= 8) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score++;

        if (score <= 2) return 1; // Weak
        if (score <= 3) return 2; // Medium
        return 3; // Strong
    }

    private String getPasswordStrengthText(String password) {
        int strength = calculatePasswordStrength(password);
        switch (strength) {
            case 1: return "Weak";
            case 2: return "Medium";
            case 3: return "Strong";
            default: return "Weak";
        }
    }

    private void resetPassword() {
        String username = usernameEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString();

        // Final validation
        if (!validateUsername() || !validateNewPassword() || !validatePasswordMatch()) {
            Toast.makeText(this, "Please fix the errors above", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent multiple clicks
        resetPasswordButton.setEnabled(false);
        resetPasswordButton.setText("RESETTING...");

        try {
            // Update password in database
            boolean isUpdated = dbHelper.updatePassword(username, newPassword);

            if (isUpdated) {
                Toast.makeText(this, "Password reset successfully!", Toast.LENGTH_LONG).show();

                // Clear all fields
                usernameEditText.setText("");
                newPasswordEditText.setText("");
                confirmPasswordEditText.setText("");

                // Navigate back to login screen after a short delay
                resetPasswordButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(ResetPassword.this, LoginActivity.class);
                        intent.putExtra("reset_success", true);
                        intent.putExtra("username", username);
                        startActivity(intent);
                        finish();
                    }
                }, 1500);

            } else {
                Toast.makeText(this, "Failed to reset password. Please try again.", Toast.LENGTH_LONG).show();
                resetPasswordButton.setEnabled(true);
                resetPasswordButton.setText("RESET PASSWORD");
            }

        } catch (Exception e) {
            Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_LONG).show();
            resetPasswordButton.setEnabled(true);
            resetPasswordButton.setText("RESET PASSWORD");
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}