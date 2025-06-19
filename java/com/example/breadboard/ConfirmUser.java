package com.example.breadboard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ConfirmUser extends AppCompatActivity {

    private TextInputEditText usernameEditText;
    private TextInputLayout usernameLayout;
    private MaterialButton confirmButton;
    private MaterialButton backToLoginButton;
    private DBHelper DB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_user);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        DB = new DBHelper(this);

        initializeViews();
        setupValidation();
        setupClickListeners();
        setupAnimations();
    }

    private void initializeViews() {
        usernameLayout = findViewById(R.id.username_layout_confirm);
        usernameEditText = findViewById(R.id.username_edittext_confirm);
        confirmButton = findViewById(R.id.btn_confirm_user);
        backToLoginButton = findViewById(R.id.btn_back_to_login_confirm);
    }

    private void setupValidation() {
        TextWatcher usernameWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateConfirmButtonState();
                if (usernameLayout.getError() != null) {
                    usernameLayout.setError(null);
                    usernameLayout.setHelperText(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        usernameEditText.addTextChangedListener(usernameWatcher);
    }

    private void updateConfirmButtonState() {
        String username = usernameEditText.getText().toString().trim();

        if (!username.isEmpty() && username.length() >= 3) {
            confirmButton.setEnabled(true);
            confirmButton.setAlpha(1.0f);
        } else {
            confirmButton.setEnabled(false);
            confirmButton.setAlpha(0.6f);
        }
    }

    private void setupClickListeners() {
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButton(v);
                verifyUsername();
            }
        });


        backToLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButton(v);
                Intent intent = new Intent(ConfirmUser.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        setupFocusAnimations();
    }

    private void animateButton(View button) {
        try {
            Animation buttonScale = AnimationUtils.loadAnimation(ConfirmUser.this, R.anim.button_scale);
            button.startAnimation(buttonScale);
        } catch (Exception e) {
        }
    }

    private void verifyUsername() {
        String username = usernameEditText.getText().toString().trim();

        if (username.isEmpty()) {
            usernameLayout.setError("Please enter your username");
            return;
        }

        if (username.length() < 3) {
            usernameLayout.setError("Username must be at least 3 characters");
            return;
        }

        confirmButton.setText("VERIFYING...");
        confirmButton.setEnabled(false);

        new Handler().postDelayed(() -> {

            Boolean userExists = DB.checkusername(username);

            if (userExists) {

                usernameLayout.setError(null);
                usernameLayout.setHelperText("Username verified ✓");
                usernameLayout.setHelperTextColor(getColorStateList(android.R.color.holo_green_dark));

                Toast.makeText(this, "Username verified! Redirecting to password reset...", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, ResetPassword.class);
                intent.putExtra("username", username);
                startActivity(intent);
                finish();
            } else {

                usernameLayout.setError("Username not found. Please check and try again.");
                usernameLayout.setHelperText(null);
                Toast.makeText(this, "Username not found in our records", Toast.LENGTH_SHORT).show();

                confirmButton.setText("CONFIRM USERNAME");
                confirmButton.setEnabled(true);
            }
        }, 1500);
    }

    private void setupFocusAnimations() {
        if (usernameEditText != null) {
            usernameEditText.setOnFocusChangeListener((v, hasFocus) -> {
                animateInputFocus(usernameLayout, hasFocus);
            });
        }
    }

    private void animateInputFocus(View view, boolean hasFocus) {
        if (view == null) return;

        if (hasFocus) {
            view.animate()
                    .scaleX(1.02f)
                    .scaleY(1.02f)
                    .setDuration(200)
                    .start();
        } else {
            view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start();
        }
    }

    private void setupAnimations() {

        View formContainer = findViewById(R.id.confirm_form_container);
        if (formContainer != null) {
            try {
                Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
                formContainer.startAnimation(slideUp);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Go back to login activity
        super.onBackPressed();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}