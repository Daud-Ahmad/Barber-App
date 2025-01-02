package com.qtt.thebarber;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.qtt.thebarber.Common.Common;
import com.qtt.thebarber.Common.LoadingDialog;

import java.util.concurrent.TimeUnit;

public class LoginWithPhoneNumberActivity extends AppCompatActivity {

    private EditText phoneNumberEditText, otpCodeEditText;
    private Button sendOtpButton, verifyOtpButton;

    private LoadingDialog loadingDialog;

    private FirebaseAuth firebaseAuth;
    private String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(this.getResources().getColor(R.color.colorAccent2));
        setContentView(R.layout.activity_login_with_phone_number);
        loadingDialog = new LoadingDialog(this);

        phoneNumberEditText = findViewById(R.id.phone_number);
        otpCodeEditText = findViewById(R.id.otp_code);
        sendOtpButton = findViewById(R.id.send_otp_button);
        verifyOtpButton = findViewById(R.id.verify_otp_button);

        firebaseAuth = FirebaseAuth.getInstance();

        sendOtpButton.setOnClickListener(v -> sendOtp());

        verifyOtpButton.setOnClickListener(v -> verifyOtp());
    }

    private void sendOtp() {
        String phoneNumber = phoneNumberEditText.getText().toString().trim();

        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingDialog.show();

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)        // Phone number to verify
                .setTimeout(120L, TimeUnit.SECONDS) // Timeout duration
                .setActivity(this)                 // Activity (for callback binding)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        runOnUiThread(() -> {
                            signInWithPhoneAuthCredential(credential);
                        });
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            Toast.makeText(LoginWithPhoneNumberActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("LoginActivity", "onVerificationFailed: ", e);
                        });
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        super.onCodeSent(s, token);
                        runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            verificationId = s;
                            otpCodeEditText.setVisibility(View.VISIBLE);
                            verifyOtpButton.setVisibility(View.VISIBLE);
                            Toast.makeText(LoginWithPhoneNumberActivity.this, "OTP sent!", Toast.LENGTH_SHORT).show();
                        });
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyOtp() {
        String code = otpCodeEditText.getText().toString().trim();

        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingDialog.show();

        if (verificationId != null) {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            signInWithPhoneAuthCredential(credential);
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        AuthResult result = task.getResult();
                        if (result != null && result.getUser() != null) {
                            Log.e("TOKEN_CLIENT_APP", "Error retrieving token", task.getException());
                            loadingDialog.dismiss();

                            Intent intent = new Intent(LoginWithPhoneNumberActivity.this, HomeActivity.class);
                            intent.putExtra(Common.IS_LOGIN, true);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        loadingDialog.dismiss();
                        Toast.makeText(this, "Login failed!", Toast.LENGTH_SHORT).show();
                        Log.e("LoginActivity", "signInWithCredential:failure", task.getException());
                    }
                });
    }
}
