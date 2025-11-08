package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

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

        progressBar = findViewById(R.id.progress_bar);
        emailEditText = findViewById(R.id.emailEditText);
        passEditText = findViewById(R.id.passEditText);
        loginBtn = findViewById(R.id.loginBtn);
        signupPageBtn = findViewById(R.id.signupPageBtn);
        googleLoginBtn = findViewById(R.id.googleLoginBtn);

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
        String pass = passEditText.getText().toString();
        if (!validate(email, pass)) return;

        changeInProgress(true);
        auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    changeInProgress(false);
                    if (task.isSuccessful()) {
                        saveUserFcmToken();
                        goToSplash();
                    } else {
                        Toast.makeText(LoginActivity.this, "Email hoặc mật khẩu sai.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void googleSignin() {
        changeInProgress(true);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                } else {
                    changeInProgress(false);
                    Toast.makeText(this, "Không lấy được tài khoản Google.", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                changeInProgress(false);
                Toast.makeText(this, "Đăng nhập Google thất bại: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        String idToken = account.getIdToken();
        if (idToken == null) {
            Toast.makeText(this, "Thiếu ID Token.", Toast.LENGTH_SHORT).show();
            changeInProgress(false);
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    changeInProgress(false);
                    if (task.isSuccessful()) {
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        String uid = auth.getCurrentUser().getUid();

                        db.collection("user").document(uid)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (!documentSnapshot.exists()) {
                                        Map<String, Object> userMap = new HashMap<>();
                                        userMap.put("name", account.getDisplayName());
                                        userMap.put("email", account.getEmail());
                                        userMap.put("fcmToken", "");
                                        db.collection("user").document(uid).set(userMap);
                                    }
                                    saveUserFcmToken();
                                    goToSplash();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(LoginActivity.this, "Không thể tạo user: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    } else {
                        Toast.makeText(LoginActivity.this, "Xác thực Google thất bại.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void changeInProgress(boolean inProgress) {
        progressBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        loginBtn.setEnabled(!inProgress);
        googleLoginBtn.setEnabled(!inProgress);
    }

    private boolean validate(String email, String pass) {
        boolean ok = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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

    private void saveUserFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        String uid = auth.getCurrentUser().getUid();
                        Log.d("FCM_TOKEN", "User token: " + token);

                        FirebaseFirestore.getInstance()
                                .collection("user")
                                .document(uid)
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> {
                                })
                                .addOnFailureListener(e -> {
                                    FirebaseFirestore.getInstance()
                                            .collection("user")
                                            .document(uid)
                                            .set(new HashMap<String, Object>() {{
                                                put("fcmToken", token);
                                            }}, com.google.firebase.firestore.SetOptions.merge());
                                });
                    }
                });
    }
}
