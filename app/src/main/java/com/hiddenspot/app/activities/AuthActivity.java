package com.hiddenspot.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.hiddenspot.app.R;
import com.hiddenspot.app.utils.FirebaseHelper;

public class AuthActivity extends AppCompatActivity {

    private boolean isLogin = true;
    private TextInputLayout tilUsername, tilEmail, tilPassword;
    private TextInputEditText etUsername, etEmail, etPassword;
    private MaterialButton btnAction;
    private TextView tvTitle, tvSubtitle, tvForgot, tvToggleLabel, tvToggleAction;
    private FirebaseHelper firebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        firebase       = FirebaseHelper.getInstance();
        tilUsername    = findViewById(R.id.til_username);
        tilEmail       = findViewById(R.id.til_email);
        tilPassword    = findViewById(R.id.til_password);
        etUsername     = findViewById(R.id.et_username);
        etEmail        = findViewById(R.id.et_email);
        etPassword     = findViewById(R.id.et_password);
        btnAction      = findViewById(R.id.btn_action);
        tvTitle        = findViewById(R.id.tv_title);
        tvSubtitle     = findViewById(R.id.tv_subtitle);
        tvForgot       = findViewById(R.id.tv_forgot);
        tvToggleLabel  = findViewById(R.id.tv_toggle_label);
        tvToggleAction = findViewById(R.id.tv_toggle_action);

        btnAction.setOnClickListener(v -> {
            String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            
            if (TextUtils.isEmpty(email)) {
                tilEmail.setError("Email is required");
                return;
            } else {
                // otherwise, clear any previous error.
                tilEmail.setError(null);
            }

            if (TextUtils.isEmpty(password)) {
                tilPassword.setError("Password is required");
                return;
            } else {
                tilPassword.setError(null);
            }

            if (!isLogin) {
                if (password.length() < 8) {
                    tilPassword.setError("Password must be at least 8 characters");
                    return;
                }
                if (!password.matches(".*\\d.*")) {
                    tilPassword.setError("Password must contain at least one digit");
                    return;
                }
                tilPassword.setError(null);

                String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
                if (TextUtils.isEmpty(username)) {
                    tilUsername.setError("Username is required");
                    return;
                } else {
                    tilUsername.setError(null);
                }
            }

            btnAction.setEnabled(false);
            btnAction.setText("Please wait...");

            if (isLogin) {
                firebase.signIn(email, password, v2 -> {
                    FirebaseUser user = firebase.getCurrentUser();
                    if (user != null && user.isEmailVerified()) {
                        // Account is verified, check if Firestore document exists
                        checkUserDocumentAndProceed(user);
                    } else if (user != null) {
                        btnAction.setEnabled(true);
                        btnAction.setText(R.string.sign_in);
                        Toast.makeText(this, "Please verify your email first. Check your inbox.", Toast.LENGTH_LONG).show();
                        user.sendEmailVerification();
                    }
                }, e -> showError(e.getMessage()));
            } else {
                String username = etUsername.getText().toString().trim();
                firebase.signUp(email, password, username, v2 -> {
                    FirebaseUser user = firebase.getCurrentUser();
                    if (user != null) {
                        user.sendEmailVerification()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Verification email sent to " + email, Toast.LENGTH_LONG).show();
                                    toggleMode(); // Switch to login mode
                                } else {
                                    showError(task.getException().getMessage());
                                }
                            });
                    }
                }, e -> showError(e.getMessage()));
            }
        });

        tvToggleAction.setOnClickListener(v -> toggleMode());

        tvForgot.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if (email.isEmpty()) { 
                tilEmail.setError("Enter your email first");
                return; 
            }
            firebase.getAuth().sendPasswordResetEmail(email)
                    .addOnSuccessListener(v2 -> Toast.makeText(this, "Reset email sent!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
        });

    }

    private void checkUserDocumentAndProceed(FirebaseUser user) {
        firebase.fetchUserDoc(user.getUid(), doc -> {
            if (doc.exists()) {
                goToMain();
            } else {
                // Verified but no document yet, create it now
                String displayName = user.getDisplayName();
                if (displayName == null || displayName.isEmpty()) displayName = "User";
                
                firebase.createUserDocument(user.getUid(), user.getEmail(), displayName,
                        v -> goToMain(), e -> showError(e.getMessage()));
            }
        }, e -> showError(e.getMessage()));
    }

    private void toggleMode() {
        isLogin = !isLogin;
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilUsername.setError(null);

        if (isLogin) {
            tvTitle.setText(R.string.welcome_back);
            tvSubtitle.setText(R.string.sign_in_subtitle);
            btnAction.setText(R.string.sign_in);
            tilUsername.setVisibility(View.GONE);
            tvForgot.setVisibility(View.VISIBLE);
            tvToggleLabel.setText(R.string.no_account);
            tvToggleAction.setText(R.string.sign_up);
        } else {
            tvTitle.setText(R.string.join_hiddenspot);
            tvSubtitle.setText(R.string.sign_up_subtitle);
            btnAction.setText(R.string.create_account);
            tilUsername.setVisibility(View.VISIBLE);
            tvForgot.setVisibility(View.GONE);
            tvToggleLabel.setText(R.string.have_account);
            tvToggleAction.setText(R.string.sign_in);
        }
        btnAction.setEnabled(true);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void showError(String msg) {
        runOnUiThread(() -> {
            btnAction.setEnabled(true);
            btnAction.setText(isLogin ? getString(R.string.sign_in) : getString(R.string.create_account));
            Toast.makeText(this, msg != null ? msg : "Authentication failed", Toast.LENGTH_LONG).show();
        });
    }
}
