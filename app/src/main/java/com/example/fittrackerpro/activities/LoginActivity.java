package com.example.fittrackerpro.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fittrackerpro.databinding.ActivityLoginBinding;
import com.example.fittrackerpro.utils.AuthMessages;
import com.example.fittrackerpro.utils.StartupRouter;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;

    // 1. Receives the result when the user picks their Google account
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account != null && account.getIdToken() != null) {
                        firebaseAuthWithGoogle(account.getIdToken());
                    } else {
                        setLoading(false);
                    }
                } catch (ApiException e) {
                    setLoading(false);
                    Toast.makeText(this,
                            "Google sign-in failed (code " + e.getStatusCode() + "). Check SHA-1 & google-services.json.",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        // 2. Configure GoogleSignInClient using default_web_client_id from google-services.json
        int webClientIdRes = getResources().getIdentifier(
                "default_web_client_id", "string", getPackageName());
        if (webClientIdRes != 0) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(webClientIdRes))
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(this, gso);
        }

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnGoogleSignIn.setOnClickListener(v -> startGoogleSignIn());
        binding.tvForgot.setOnClickListener(v -> {
            Intent i = new Intent(this, ForgotPasswordActivity.class);
            i.putExtra("email", binding.etEmail.getText().toString().trim());
            startActivity(i);
        });
        binding.tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    // 3. Opens the Google account picker popup
    private void startGoogleSignIn() {
        if (googleSignInClient == null) {
            Toast.makeText(this,
                    "Enable Google Sign-In in Firebase Console and re-download google-services.json first.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        setLoading(true);
        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    // 4. Exchanges the Google ID token for a Firebase Auth session
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        StartupRouter.route(this);
                    } else {
                        Toast.makeText(this,
                                AuthMessages.from(task.getException()),
                                Toast.LENGTH_LONG).show();
                    }
                });
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
                        StartupRouter.route(this);
                    } else {
                        Toast.makeText(this,
                                AuthMessages.from(task.getException()),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.btnGoogleSignIn.setEnabled(!loading);
    }

    private void goToDashboard() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}