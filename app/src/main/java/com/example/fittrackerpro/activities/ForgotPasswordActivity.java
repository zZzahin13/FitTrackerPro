package com.example.fittrackerpro.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fittrackerpro.databinding.ActivityForgotPasswordBinding;
import com.example.fittrackerpro.utils.AuthMessages;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Email typed on the login screen arrives as an Intent extra
        String prefill = getIntent().getStringExtra("email");
        if (prefill != null) {
            binding.etEmail.setText(prefill);
        }

        binding.btnSend.setOnClickListener(v -> sendReset());
        binding.tvBack.setOnClickListener(v -> finish());
    }

    private void sendReset() {
        String email = binding.etEmail.getText().toString().trim();
        binding.tilEmail.setError(null);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Enter a valid email");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSend.setEnabled(false);

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSend.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "If an account exists for this email, a reset link has been sent.",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, AuthMessages.from(task.getException()),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}