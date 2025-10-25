package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.example.newEcom.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashAuth";
    private static final String ADMIN_EMAIL = "buitrongtri2004@gmail.com";

    LottieAnimationView lottieAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.wtf(TAG, "onCreate() ENTER"); // luôn in
        setContentView(R.layout.activity_splash);

        lottieAnimation = findViewById(R.id.lottieAnimationView);
        if (lottieAnimation != null) lottieAnimation.playAnimation();

        new Handler().postDelayed(() -> {
            Log.wtf(TAG, "postDelayed RUN"); // luôn in

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Log.wtf(TAG, "No current user -> LoginActivity");
                gotoAndClear(LoginActivity.class);
                return;
            }

            // Log sơ bộ trước khi reload
            String e0 = user.getEmail();
            Log.wtf(TAG, "Before reload email=[" + e0 + "] uid=" + user.getUid());

            user.reload().addOnCompleteListener((@NonNull Task<Void> task) -> {
                FirebaseUser cur = FirebaseAuth.getInstance().getCurrentUser();
                String raw = (cur != null) ? cur.getEmail() : null;
                String email = (raw == null) ? "" : raw.trim().toLowerCase();

                Log.wtf(TAG, "reload() done success=" + task.isSuccessful());
                Log.wtf(TAG, "uid=" + (cur != null ? cur.getUid() : "null"));
                Log.wtf(TAG, "email(raw)=[" + raw + "]");
                Log.wtf(TAG, "email(norm)=[" + email + "]");
                Log.wtf(TAG, "emailVerified=" + (cur != null && cur.isEmailVerified()));
                if (cur != null) {
                    for (UserInfo info : cur.getProviderData()) {
                        Log.wtf(TAG, "provider=" + info.getProviderId() + ", pEmail=" + info.getEmail());
                    }
                }


                Class<?> dest;
                if (ADMIN_EMAIL.equals(email)) {
                    Log.wtf(TAG, "Matched ADMIN -> AdminActivity");
                    dest = AdminActivity.class;
                } else {
                    Log.wtf(TAG, "Not admin -> MainActivity");
                    dest = MainActivity.class;
                }
                gotoAndClear(dest);
            });
        }, 1500);
    }

    private void gotoAndClear(Class<?> cls) {
        Intent i = new Intent(SplashActivity.this, cls);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        Log.wtf(TAG, "startActivity(" + cls.getSimpleName() + "), finish()");
        finish();
    }

    private void toast(String m) {
        Toast.makeText(this, "[Splash] " + m, Toast.LENGTH_SHORT).show();
    }
}