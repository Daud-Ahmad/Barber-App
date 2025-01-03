package com.qtt.thebarber;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.qtt.thebarber.Common.Common;
import com.qtt.thebarber.Common.LoadingDialog;
import com.qtt.thebarber.Interface.IUpdateProfileListener;
import com.qtt.thebarber.databinding.ActivityUpdateProfileBinding;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UpdateProfileActivity extends AppCompatActivity implements IUpdateProfileListener {
    ActivityUpdateProfileBinding binding;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    Uri fileUri;
    private LoadingDialog dialog;
    StorageReference storageReference;
    IUpdateProfileListener iUpdateProfileListener;
    private String currentPhotoPath;

    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    File file = new File(currentPhotoPath);
                    if (file.exists()) {
                        binding.imgUserAvatar.setImageURI(Uri.fromFile(file));
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateProfileBinding.inflate(getLayoutInflater());
        getWindow().setStatusBarColor(this.getResources().getColor(R.color.colorAccent2));

        dialog = new LoadingDialog(this);

        initView();
        setContentView(binding.getRoot());
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                fileUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                takePictureLauncher.launch(takePictureIntent);
            }
        }
    }

    private void initView() {
        iUpdateProfileListener = this;

        binding.imgBack.setOnClickListener(v -> finish());

        binding.edtUserName.setText(Common.currentUser.getName());
        binding.edtUserAddress.setText(Common.currentUser.getAddress());
        binding.edtUserPhone.setText(Common.currentUser.getPhoneNumber());

        if (Common.currentUser.getAvatar() != null && !Common.currentUser.getAvatar().isEmpty()) {
            Picasso.get().load(Common.currentUser.getAvatar()).error(R.drawable.user_avatar).into(binding.imgUserAvatar);
        } else {
            Picasso.get().load(Common.currentUser.getAvatar()).error(R.drawable.user_avatar).into(binding.imgUserAvatar);
        }

        binding.imgAddAvatar.setOnClickListener(v -> {
            if (checkAndRequestCameraPermission()) {
                dispatchTakePictureIntent();
            }
        });

        binding.btnUpdate.setOnClickListener(v -> {

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("name", binding.edtUserName.getText().toString());
            updateData.put("address", binding.edtUserAddress.getText().toString());

                dialog.show();
                FirebaseFirestore.getInstance().collection("User")
                        .document(Common.currentUser.getPhoneNumber())
                        .update(updateData)
                        .addOnCompleteListener(task13 -> {
                            Log.d("Update_profile", "update name: successfully");

                            upLoadPicture(fileUri);

                            dialog.dismiss();
                        }).addOnFailureListener(e -> {
                            iUpdateProfileListener.OnUpdateProfileFailed(e.getMessage());
                            dialog.dismiss();
                        });
        });
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void upLoadPicture(Uri fileUri) {
        if (fileUri != null) {
            dialog.show();

            String fileName = Common.getFileName(getContentResolver(), fileUri);
            String path = new StringBuilder("User_Avatar/").append(fileName).toString();

            storageReference = FirebaseStorage.getInstance().getReference(path);

            UploadTask uploadTask = storageReference.putFile(fileUri);

            Task<Uri> task = uploadTask.continueWithTask(task1 -> {
                if (!task1.isSuccessful()) {
                    Toast.makeText(UpdateProfileActivity.this, "Failed to upload picture!", Toast.LENGTH_SHORT).show();
                }

                return storageReference.getDownloadUrl();

            }).addOnCompleteListener(task12 -> {
                if (task12.isSuccessful()) {
                    String url = task12.getResult().toString().substring(0, task12.getResult().toString().indexOf("&token"));
                    Log.d("AAAAA", "download: " + url);

                    FirebaseFirestore.getInstance().collection("User")
                            .document(Common.currentUser.getPhoneNumber())
                            .update("avatar", url)
                            .addOnCompleteListener(task13 -> {
                                Log.d("Update_profile", "upLoadPicture: successfully " + url);
                                Common.currentUser.setAvatar(url);
                               iUpdateProfileListener.onUpdateProfileSuccess(true);
                                dialog.dismiss();
                            }).addOnFailureListener(e -> {iUpdateProfileListener.OnUpdateProfileFailed(e.getMessage());
                            dialog.dismiss();
                            });
                }
            }).addOnFailureListener(e -> {
                dialog.dismiss();
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            iUpdateProfileListener.onUpdateProfileSuccess(true);
        }
    }

    @Override
    public void onUpdateProfileSuccess(boolean isSuccess) {
        if (isSuccess) {
            Common.currentUser.setName(binding.edtUserName.getText().toString());
            Common.currentUser.setAddress(binding.edtUserAddress.getText().toString());

            finish();
        }
    }

    @Override
    public void OnUpdateProfileFailed(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is required to use the camera.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}