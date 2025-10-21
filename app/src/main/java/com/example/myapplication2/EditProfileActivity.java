package com.example.myapplication2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView imageProfile;
    private EditText editName;
    private Button buttonSelectImage, buttonSave;

    private Uri selectedImageUri;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        imageProfile = findViewById(R.id.image_profile);
        editName = findViewById(R.id.edit_name);
        buttonSelectImage = findViewById(R.id.button_select_image);
        buttonSave = findViewById(R.id.button_save);
        prefs = getSharedPreferences("user_profile", MODE_PRIVATE);

        // Load existing data
        String savedName = prefs.getString("name", "");
        String imagePath = prefs.getString("image_path", null);
        editName.setText(savedName);

        if (imagePath != null) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                Glide.with(this)
                        .load(imgFile)
                        .skipMemoryCache(true)// ⚠️ 避免快取
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                        .circleCrop()
                        .placeholder(R.drawable.profile_placeholder)
                        .into(imageProfile);
            } else {
                imageProfile.setImageResource(R.drawable.profile_placeholder);
            }
        } else {
            imageProfile.setImageResource(R.drawable.profile_placeholder);
        }



        // Select image
        buttonSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        buttonSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "請輸入名稱", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("name", name);

            // ⚠️ 儲存頭像檔案路徑而不是 URI
            if (selectedImageUri != null) {
                String Sname = name.replaceAll("[^a-zA-Z0-9]", "_");
                File file = new File(getFilesDir(), "profile_images/" + Sname + ".jpg");
                editor.putString("image_path", file.getAbsolutePath());
                // ✅ 即時更新 UI 頭像
                Glide.with(this)
                        .load(file)
                        .circleCrop()
                        .placeholder(R.drawable.profile_placeholder)
                        .into(imageProfile);

            }

            editor.apply();

            Toast.makeText(this, "已儲存個人資料", Toast.LENGTH_SHORT).show();
            finish();
        });

    }

    // Pick image result handler
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        String currentName = editName.getText().toString();
                        File savedFile = saveImageToInternalStorage(uri, currentName);
                        selectedImageUri = Uri.fromFile(savedFile);


                        Glide.with(this)
                                .load(savedFile)
                                .circleCrop()
                                .placeholder(R.drawable.profile_placeholder)
                                .into(imageProfile);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "無法儲存圖片", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // Save picked image to internal storage
    private File saveImageToInternalStorage(Uri imageUri, String userName) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();

        File directory = new File(getFilesDir(), "profile_images");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String safeName = userName.replaceAll("[^a-zA-Z0-9]", "_");
        File file = new File(directory, safeName + ".jpg");

        FileOutputStream fos = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.close();

        return file;
    }


}
