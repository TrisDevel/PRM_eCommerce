package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_GOOGLE = 101;

    ProgressBar progressBar;
    EditText emailEditText, passEditText;
    ImageView loginBtn;
    TextView signupPageBtn;
    Button googleLoginBtn;

    FirebaseAuth auth;
    GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        progressBar     = findViewById(R.id.progress_bar);
        emailEditText   = findViewById(R.id.emailEditText);
        passEditText    = findViewById(R.id.passEditText);
        loginBtn        = findViewById(R.id.loginBtn);
        signupPageBtn   = findViewById(R.id.signupPageBtn);
        googleLoginBtn  = findViewById(R.id.googleLoginBtn);

        auth = FirebaseAuth.getInstance();

        loginBtn.setOnClickListener(v -> loginUser());
        signupPageBtn.setOnClickListener(v -> startActivity(new Intent(this, SignupActivity.class)));

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleLoginBtn.setOnClickListener(v -> googleSignin());
    }

    private void loginUser() {
        String email = emailEditText.getText().toString();
        String pass  = passEditText.getText().toString();
        if (!validate(email, pass)) return;

        changeInProgress(true);
        auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    changeInProgress(false);
                    if (task.isSuccessful()) {
                        // KHÔNG điều hướng Admin/Main ở đây!
                        goToSplash();
                    } else {
                        Toast.makeText(LoginActivity.this, "Email hoặc mật khẩu sai.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void googleSignin() {
        Intent intent = googleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_GOOGLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (Exception e) {
                Toast.makeText(this, "Google Sign-In thất bại.", Toast.LENGTH_SHORT).show();
                changeInProgress(false);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null) {
            Toast.makeText(this, "Thiếu ID token.", Toast.LENGTH_SHORT).show();
            return;
        }
        changeInProgress(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, (OnCompleteListener<AuthResult>) task -> {
                    changeInProgress(false);
                    if (task.isSuccessful()) {
                        // KHÔNG điều hướng Admin/Main ở đây!
                        goToSplash();
                    } else {
                        Toast.makeText(LoginActivity.this, "Xác thực Google thất bại.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void changeInProgress(boolean inProgress){
        progressBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        loginBtn.setEnabled(!inProgress);
        googleLoginBtn.setEnabled(!inProgress);
    }

    private boolean validate(String email, String pass){
        boolean ok = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            emailEditText.setError("Email không hợp lệ");
            ok = false;
        }
        if (pass.length() < 6) {
            passEditText.setError("Mật khẩu tối thiểu 6 ký tự");
            ok = false;
        }
        return ok;
    }

    private void goToSplash() {
        Intent i = new Intent(LoginActivity.this, SplashActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}