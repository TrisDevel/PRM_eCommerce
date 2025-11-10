package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.newEcom.model.UserModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;

public class SignupActivity extends AppCompatActivity {
    ProgressBar progressBar;
    EditText nameEditText, emailEditText, passEditText;
    ImageView nextBtn;
    TextView loginPageBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        progressBar = findViewById(R.id.progress_bar);
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passEditText = findViewById(R.id.passEditText);
        nextBtn = findViewById(R.id.nextBtn);
        loginPageBtn = findViewById(R.id.loginPageBtn);

        nextBtn.setOnClickListener(v -> createAccount());

        loginPageBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void createAccount() {
        String email = emailEditText.getText().toString();
        String pass = passEditText.getText().toString();
//        String confPass = confPassEditText.getText().toString();

        boolean isValidated = validate(email, pass);
        if (!isValidated)
            return;

        createAccountInFirebase(email,pass);
    }

    void createAccountInFirebase(String email, String pass) {
        changeInProgress(true);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this,
                task -> {
                    if (task.isSuccessful()) {
                        // Authentication successful, now create user document in Firestore
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser == null) {
                            changeInProgress(false);
                            Toast.makeText(SignupActivity.this, "Failed to get user data.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String fullName = nameEditText.getText().toString();

                        // Create a UserModel object with default values
                        UserModel userModel = new UserModel(
                                firebaseUser.getUid(),
                                email,
                                fullName,
                                null, // phoneNumber
                                null, // address
                                null, // profileImage
                                "user", // role
                                true, // isActive
                                false, // isEmailVerified
                                Timestamp.now(), // createdAt
                                null, // lastLoginAt
                                0, // totalOrders
                                0, // totalSpent
                                null, // preferredLanguage
                                null  // timezone
                        );

                        // Save the UserModel to Firestore
                        FirebaseUtil.getUsers().document(firebaseUser.getUid()).set(userModel)
                                .addOnSuccessListener(aVoid -> {
                                    // Firestore write successful
                                    changeInProgress(false);
                                    Toast.makeText(SignupActivity.this, "Successfully created account, check email to verify", Toast.LENGTH_SHORT).show();

                                    // Send verification email
                                    firebaseUser.sendEmailVerification();

                                    // Update DisplayName in Auth (optional but good practice)
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(fullName).build();
                                    firebaseUser.updateProfile(profileUpdates);

                                    // Sign out to force user to log in again
                                    firebaseAuth.signOut();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    // Firestore write failed
                                    changeInProgress(false);
                                    Toast.makeText(SignupActivity.this, "Error: Failed to save user data.", Toast.LENGTH_SHORT).show();
                                    // Optional: Delete the created auth user to prevent orphaned accounts
                                    // firebaseUser.delete();
                                });

                    } else {
                        // Authentication failed
                        changeInProgress(false);
                        Toast.makeText(SignupActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    void changeInProgress(boolean inProgress){
        if (inProgress){
            progressBar.setVisibility(View.VISIBLE);
            nextBtn.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            nextBtn.setVisibility(View.VISIBLE);
        }
    }

    boolean validate(String email, String pass){
        int flag=0;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            emailEditText.setError("Email is invalid");
            flag=1;
        }
        if (pass.length() < 6){
            passEditText.setError("Password must be of six characters");
            flag=1;
        }
//        if (!pass.equals(confPass)){
//            confPassEditText.setError("Confirm Password not matched");
//            flag=1;
//        }
        return flag == 0;
    }
}