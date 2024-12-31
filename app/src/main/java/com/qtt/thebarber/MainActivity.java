package com.qtt.thebarber;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.qtt.thebarber.Common.Common;
import com.qtt.thebarber.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private ActivityMainBinding binding;

    void loginUser() {
        startActivity(new Intent(this, LoginWithPhoneNumberActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        getWindow().setStatusBarColor(this.getResources().getColor(R.color.colorBackground));

        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = firebaseAuth1 -> {
            FirebaseUser user = firebaseAuth1.getCurrentUser();

            if (user != null) {
                MainActivity.this.checkUserFromFirebase(user);
            }
        };

        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {

                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {

                            FirebaseMessaging.getInstance().getToken()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            String token = task.getResult();
                                            Common.updateToken(getBaseContext(), token);

                                            Log.d("TOKEN_CLIENT_APP", token);

                                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                                            intent.putExtra(Common.IS_LOGIN, true);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Failed to get FCM token", Toast.LENGTH_SHORT).show();
                                            Log.e("TOKEN_CLIENT_APP", "Error retrieving token", task.getException());

                                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                                            intent.putExtra(Common.IS_LOGIN, true);
                                            startActivity(intent);
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.e("TOKEN_CLIENT_APP", "Error retrieving token", e);

                                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                                        intent.putExtra(Common.IS_LOGIN, true);
                                        startActivity(intent);
                                        finish();
                                    });

//                            FirebaseInstanceId.getInstance()
//                                    .getInstanceId()
//                                    .addOnCompleteListener(task -> {
//                                        if (task.isSuccessful()) {
//                                            Common.updateToken(getBaseContext(), task.getResult().getToken());
//
//                                            Log.d("TOKEN_CLIENT_APP", task.getResult().getToken());
//
//                                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
//                                            intent.putExtra(Common.IS_LOGIN, true);
//                                            startActivity(intent);
//                                            finish();
//                                        }
//                                    }).addOnFailureListener(e -> {
//                                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
//                                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
//                                        intent.putExtra(Common.IS_LOGIN, true);
//                                        startActivity(intent);
//                                        finish();
//                                    });

                        } else {
                            setContentView(binding.getRoot());

                            binding.btnLogin.setOnClickListener(v -> loginUser());
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void checkUserFromFirebase(FirebaseUser user) {

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Common.updateToken(getBaseContext(), token);

                        Log.d("TOKEN_CLIENT_APP", token);

                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                        intent.putExtra(Common.IS_LOGIN, true);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to get FCM token", Toast.LENGTH_SHORT).show();
                        Log.e("TOKEN_CLIENT_APP", "Error retrieving token", task.getException());

                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                        intent.putExtra(Common.IS_LOGIN, true);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("TOKEN_CLIENT_APP", "Error retrieving token", e);

                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    intent.putExtra(Common.IS_LOGIN, true);
                    startActivity(intent);
                    finish();
                });

//        FirebaseInstanceId.getInstance()
//                .getInstanceId()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        Common.updateToken(getBaseContext(), task.getResult().getToken());
//
//                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
//                        intent.putExtra(Common.IS_LOGIN, true);
//                        startActivity(intent);
//                        finish();
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
//                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
//                intent.putExtra(Common.IS_LOGIN, true);
//                startActivity(intent);
//                finish();
//            }
//        });
    }

}
