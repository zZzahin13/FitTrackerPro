package com.example.fittrackerpro;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fittrackerpro.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.tvForgot.setOnClickListener(v -> sendReset());
        binding.tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Enter a valid email");
            return;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError("At least 6 characters");
            return;
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        goToDashboard();
                    } else {
                        Toast.makeText(this,
                                "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendReset() {
        String email = binding.etEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Type your email first");
            return;
        }
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task ->
                Toast.makeText(this,
                        task.isSuccessful() ? "Reset email sent" : "Could not send email",
                        Toast.LENGTH_LONG).show());
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
    }

    private void goToDashboard() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}