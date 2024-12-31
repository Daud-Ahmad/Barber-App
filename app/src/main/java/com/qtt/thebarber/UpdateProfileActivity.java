package com.qtt.thebarber;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.qtt.thebarber.Common.Common;
import com.qtt.thebarber.Interface.IUpdateProfileListener;
import com.qtt.thebarber.databinding.ActivityUpdateProfileBinding;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class UpdateProfileActivity extends AppCompatActivity implements IUpdateProfileListener {
    ActivityUpdateProfileBinding binding;
    private static final int MY_CAMERA_REQUEST_CODE = 911;
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    Uri fileUri;
//    AlertDialog dialog;
    StorageReference storageReference;
    IUpdateProfileListener iUpdateProfileListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateProfileBinding.inflate(getLayoutInflater());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(this.getResources().getColor(R.color.colorAccent2));
        }

//        dialog = new SpotsDialog.Builder()
//                .setCancelable(false)
//                .setContext(this)
//                .build();

        initView();
        setContentView(binding.getRoot());
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
//            checkStoragePermission();
            onClickImageUpdate();
        });

        binding.btnUpdate.setOnClickListener(v -> {

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("name", binding.edtUserName.getText().toString());
            updateData.put("address", binding.edtUserAddress.getText().toString());

//                dialog.show();
                FirebaseFirestore.getInstance().collection("User")
                        .document(Common.currentUser.getPhoneNumber())
                        .update(updateData)
                        .addOnCompleteListener(task13 -> {
                            Log.d("Update_profile", "update name: successfully");

                            upLoadPicture(fileUri);

//                            dialog.dismiss();
                        }).addOnFailureListener(e -> {
                            iUpdateProfileListener.OnUpdateProfileFailed(e.getMessage());
//                            dialog.dismiss();
                        });
        });
    }

    private void onClickImageUpdate(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        fileUri = getOutputMediaFileUri();

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        startActivityForResult(intent, MY_CAMERA_REQUEST_CODE);
    }

    private void upLoadPicture(Uri fileUri) {
        if (fileUri != null) {
//            dialog.show();

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
//                                dialog.dismiss();
                            }).addOnFailureListener(e -> {iUpdateProfileListener.OnUpdateProfileFailed(e.getMessage());
//                            dialog.dismiss();
                            });
                }
            }).addOnFailureListener(e -> {
//                dialog.dismiss();
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            iUpdateProfileListener.onUpdateProfileSuccess(true);
        }
    }

    private Uri getOutputMediaFileUri() {
        return Uri.fromFile(getOutputMediaFile());
    }

    private File getOutputMediaFile() {
        File mediaDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "TheBarberApp");

        if (!mediaDir.exists()) {
            mediaDir.mkdir();
        }

        String time_tamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaDir.getPath() + File.separator + "IMG_" + time_tamp +
                "_" + new Random().nextInt() + ".jpg");

        return mediaFile;
    }


    private void checkStoragePermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API level 33) and above
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_STORAGE_PERMISSION);
            } else {
                onClickImageUpdate();
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            } else {
                onClickImageUpdate();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onClickImageUpdate();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            Bitmap bitmap = null;
            ExifInterface exifInterface = null;

            try {
                // Use ContentResolver to open an InputStream
//                InputStream inputStream = getContentResolver().openInputStream(fileUri);
//                bitmap = BitmapFactory.decodeStream(inputStream);
                bitmap = loadImageFromMediaStore(fileUri.getPath());
                exifInterface = new ExifInterface(getContentResolver().openInputStream(fileUri));

                int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

                Bitmap rotateBitmap = null;
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotateBitmap = rotateBitmap(bitmap, 90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotateBitmap = rotateBitmap(bitmap, 180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotateBitmap = rotateBitmap(bitmap, 270);
                        break;
                    case ExifInterface.ORIENTATION_NORMAL:
                    default:
                        rotateBitmap = bitmap;
                        break;
                }

                binding.imgUserAvatar.setImageBitmap(rotateBitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap loadImageFromMediaStore(String filePath) {
        Bitmap bitmap = null;
        try {
            // Query MediaStore for the content URI of the file
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.Images.Media._ID};
            String selection = MediaStore.Images.Media.DATA + "=?";
            String[] selectionArgs = new String[]{filePath};

            Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                long id = cursor.getLong(idIndex);

                // Build the content URI for the specific image
                Uri contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));

                // Decode the bitmap
                InputStream inputStream = getContentResolver().openInputStream(contentUri);
                bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
        return bitmap;
    }


    private Bitmap rotateBitmap(Bitmap bitmap, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
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
}