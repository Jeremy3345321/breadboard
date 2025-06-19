package com.example.breadboard;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    // Login views
    private TextInputEditText loginUsernameEditText;
    private TextInputEditText loginPasswordEditText;
    private TextInputLayout loginUsernameLayout;
    private TextInputLayout loginPasswordLayout;
    private MaterialButton loginButton;
    private MaterialButton switchToRegisterButton;
    private TextView forgotPasswordText;
    private View loginFormContainer;

    // Database helper
    private DBHelper DB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize database
        DB = new DBHelper(this);

        initializeViews();
        setupPasswordValidation();
        setupClickListeners();
        setupAnimations();

        // Update button state on startup
        updateLoginButtonState();
    }

    private void initializeViews() {
        // Initialize login views
        loginFormContainer = findViewById(R.id.login_form_container);
        loginUsernameLayout = findViewById(R.id.username_layout_login);
        loginPasswordLayout = findViewById(R.id.password_layout_login);
        loginUsernameEditText = findViewById(R.id.username_edittext_login);
        loginPasswordEditText = findViewById(R.id.password_edittext_login);
        loginButton = findViewById(R.id.btn_login);
        switchToRegisterButton = findViewById(R.id.btn_switch_to_register);
        forgotPasswordText = findViewById(R.id.forgot_password_text);
    }

    private void setupPasswordValidation() {
        // Login form validation
        TextWatcher loginWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLoginButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        loginUsernameEditText.addTextChangedListener(loginWatcher);
        loginPasswordEditText.addTextChangedListener(loginWatcher);
    }

    private void updateLoginButtonState() {
        String username = loginUsernameEditText.getText().toString().trim();
        String password = loginPasswordEditText.getText().toString().trim();

        if (!username.isEmpty() && !password.isEmpty()) {
            loginButton.setEnabled(true);
            loginButton.setAlpha(1.0f);
        } else {
            loginButton.setEnabled(false);
            loginButton.setAlpha(0.6f);
        }
    }

    private void setupClickListeners() {
        // Login button click
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButton(v);
                performLogin();
            }
        });

        // Switch to register form (navigate to SigninActivity)
        switchToRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButton(v);
                navigateToRegister();
            }
        });

        // Forgot password text click
        forgotPasswordText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateText(v);
                navigateToForgotPassword();
            }
        });

        setupFocusAnimations();
    }

    private void animateButton(View button) {
        try {
            Animation buttonScale = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.button_scale);
            button.startAnimation(buttonScale);
        } catch (Exception e) {
            // Animation file not found, continue without animation
        }
    }

    private void animateText(View textView) {
        // Simple scale animation for the text view
        textView.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    textView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void performLogin() {
        String username = loginUsernameEditText.getText().toString().trim();
        String password = loginPasswordEditText.getText().toString().trim();

        // Basic validation
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        loginButton.setText("SIGNING IN...");
        loginButton.setEnabled(false);

        // Simulate network delay for better UX
        new Handler().postDelayed(() -> {
            // Check credentials
            Boolean checkCredentials = DB.checkusernamepassword(username, password);

            if (checkCredentials) {
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();

                // Login the user
                UserAuthentication userAuth = UserAuthentication.getInstance(this);
                userAuth.loginUser(username);

                // Redirect to HomeActivity
                Intent intent = new Intent(this, HomeActivity.class);
                intent.putExtra("username", username);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                // Reset button state
                loginButton.setText("SIGN IN");
                loginButton.setEnabled(true);
                updateLoginButtonState();
            }
        }, 1000);
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, SigninActivity.class);
        startActivity(intent);
    }

    private void navigateToForgotPassword() {
        Intent intent = new Intent(this, ConfirmUser.class);
        startActivity(intent);
    }

    private void clearLoginErrors() {
        if (loginUsernameLayout != null) {
            loginUsernameLayout.setError(null);
            loginUsernameLayout.setHelperText(null);
        }
        if (loginPasswordLayout != null) {
            loginPasswordLayout.setError(null);
            loginPasswordLayout.setHelperText(null);
        }
    }

    private void setupFocusAnimations() {
        // Login form focus animations
        if (loginUsernameEditText != null) {
            loginUsernameEditText.setOnFocusChangeListener((v, hasFocus) -> {
                animateInputFocus(loginUsernameLayout, hasFocus);
            });
        }

        if (loginPasswordEditText != null) {
            loginPasswordEditText.setOnFocusChangeListener((v, hasFocus) -> {
                animateInputFocus(loginPasswordLayout, hasFocus);
            });
        }
    }

    private void animateInputFocus(View view, boolean hasFocus) {
        if (view == null) return;

        if (hasFocus) {
            ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.02f).setDuration(200).start();
            ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.02f).setDuration(200).start();
        } else {
            ObjectAnimator.ofFloat(view, "scaleX", 1.02f, 1.0f).setDuration(200).start();
            ObjectAnimator.ofFloat(view, "scaleY", 1.02f, 1.0f).setDuration(200).start();
        }
    }

    private void setupAnimations() {
        // Add any entrance animations here
        // You can add slide-in animations similar to MainActivity
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear any errors when returning to this activity
        clearLoginErrors();
        updateLoginButtonState();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}