package com.example.breadboard;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SigninActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText, passwordEditText, repasswordEditText;
    private TextInputLayout usernameLayout, passwordLayout, repasswordLayout;
    private MaterialButton signupButton, signinButton;
    private View strengthBar1, strengthBar2, strengthBar3;
    private android.widget.TextView strengthText;
    private View passwordStrengthLayout;
    private CardView mainCard;
    private DBHelper DB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        initializeViews();
        setupDatabase();
        setupPasswordValidation();
        setupClickListeners();
        animateCardIn();
    }

    private void initializeViews() {
        mainCard = findViewById(R.id.main_card);

        // Text Input Layouts
        usernameLayout = findViewById(R.id.Username);
        passwordLayout = findViewById(R.id.Password);
        repasswordLayout = findViewById(R.id.Repassword);

        // Edit Text fields
        usernameEditText = findViewById(R.id.UsernameEditText);
        passwordEditText = findViewById(R.id.PasswordEditText);
        repasswordEditText = findViewById(R.id.RepasswordEditText);

        // Buttons
        signupButton = findViewById(R.id.btnsignup);
        signinButton = findViewById(R.id.btnsignin);

        // Password strength indicators
        passwordStrengthLayout = findViewById(R.id.password_strength_layout);
        strengthBar1 = findViewById(R.id.strength_bar_1);
        strengthBar2 = findViewById(R.id.strength_bar_2);
        strengthBar3 = findViewById(R.id.strength_bar_3);
        strengthText = findViewById(R.id.strength_text);
    }

    private void setupDatabase() {
        DB = new DBHelper(this);
    }

    private void setupPasswordValidation() {
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateUsername(s.toString());
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword(s.toString());
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        repasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePasswordMatch();
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void validateUsername(String username) {
        if (username.isEmpty()) {
            usernameLayout.setError(null);
        } else if (username.length() < 3) {
            usernameLayout.setError("Username must be at least 3 characters");
        } else if (!username.matches("^[a-zA-Z0-9_]+$")) {
            usernameLayout.setError("Username can only contain letters, numbers, and underscores");
        } else {
            usernameLayout.setError(null);
        }
    }

    private void validatePassword(String password) {
        if (password.isEmpty()) {
            passwordStrengthLayout.setVisibility(View.GONE);
            passwordLayout.setError(null);
            return;
        }

        passwordStrengthLayout.setVisibility(View.VISIBLE);

        int strength = calculatePasswordStrength(password);
        updatePasswordStrengthUI(strength);

        if (!isPasswordValid(password)) {
            passwordLayout.setError("Password must be 8+ characters with 1 uppercase and 1 number");
        } else {
            passwordLayout.setError(null);
        }
    }

    private int calculatePasswordStrength(String password) {
        int strength = 0;

        // Length check (8+ characters)
        if (password.length() >= 8) strength++;

        // Uppercase check
        if (password.matches(".*[A-Z].*")) strength++;

        // Number check
        if (password.matches(".*[0-9].*")) strength++;

        return strength;
    }

    private void updatePasswordStrengthUI(int strength) {
        // Reset all bars to default color
        strengthBar1.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        strengthBar2.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        strengthBar3.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

        switch (strength) {
            case 1:
                strengthBar1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                strengthText.setText("Weak");
                strengthText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                break;
            case 2:
                strengthBar1.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
                strengthBar2.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
                strengthText.setText("Medium");
                strengthText.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                break;
            case 3:
                strengthBar1.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                strengthBar2.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                strengthBar3.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                strengthText.setText("Strong");
                strengthText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                break;
            default:
                strengthText.setText("Weak");
                strengthText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                break;
        }
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[0-9].*");
    }

    private void validatePasswordMatch() {
        String password = passwordEditText.getText().toString();
        String repassword = repasswordEditText.getText().toString();

        if (!repassword.isEmpty()) {
            if (!password.equals(repassword)) {
                repasswordLayout.setError("Passwords do not match");
            } else {
                repasswordLayout.setError(null);
            }
        } else {
            repasswordLayout.setError(null);
        }
    }

    private void validateForm() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String repassword = repasswordEditText.getText().toString();

        boolean isUsernameValid = !username.isEmpty() && username.length() >= 3 &&
                username.matches("^[a-zA-Z0-9_]+$");
        boolean isPasswordValid = isPasswordValid(password);
        boolean isPasswordMatching = password.equals(repassword) && !repassword.isEmpty();
        boolean isFormValid = isUsernameValid && isPasswordValid && isPasswordMatching;

        signupButton.setEnabled(isFormValid);
        signupButton.setAlpha(isFormValid ? 1.0f : 0.5f);
    }

    private void setupClickListeners() {
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerUser();
            }
        });

        signinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String repassword = repasswordEditText.getText().toString();

        // Final validation before registration
        if (username.isEmpty() || password.isEmpty() || repassword.isEmpty()) {
            showToast("Please enter all fields");
            return;
        }

        if (!isPasswordValid(password)) {
            showToast("Password must be 8+ characters with 1 uppercase and 1 number");
            return;
        }

        if (!password.equals(repassword)) {
            showToast("Passwords do not match");
            return;
        }

        // Disable button to prevent multiple clicks
        signupButton.setEnabled(false);
        signupButton.setText("CREATING...");

        try {
            Boolean userExists = DB.checkusername(username);

            if (!userExists) {
                Boolean success = DB.insertData(username, password);

                if (success) {
                    showToast("Registered successfully");

                    // Use UserAuthentication to manage user session
                    UserAuthentication userAuth = UserAuthentication.getInstance(SigninActivity.this);
                    userAuth.loginUser(username);

                    // Navigate to HomeActivity
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    showToast("Registration failed. Please try again.");
                    resetButtonState();
                }
            } else {
                showToast("User already exists");
                usernameLayout.setError("Username already exists");
                resetButtonState();
            }
        } catch (Exception e) {
            showToast("An error occurred. Please try again.");
            resetButtonState();
        }
    }

    private void resetButtonState() {
        signupButton.setEnabled(true);
        signupButton.setText("CREATE ACCOUNT");
        validateForm(); // Re-validate to set proper button state
    }

    private void showToast(String message) {
        Toast.makeText(SigninActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void animateCardIn() {
        // Animate card sliding up and fading in
        ObjectAnimator translateY = ObjectAnimator.ofFloat(mainCard, "translationY", 50f, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mainCard, "alpha", 0f, 1f);

        translateY.setDuration(600);
        alpha.setDuration(600);

        translateY.start();
        alpha.start();
    }
}