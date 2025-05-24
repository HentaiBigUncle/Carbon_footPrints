package com.example.myapplication2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import com.bumptech.glide.Glide;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgProfile;
    private TextView txtUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); // 你自己的 XML 檔名

        imgProfile = findViewById(R.id.img_profile);       // 對應你的 XML 中的 ID
        txtUsername = findViewById(R.id.txt_username);

        //loadUserProfile();
        Button profileBtn = findViewById(R.id.btn_edit_profile);
        profileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadUserProfile() {
        SharedPreferences prefs = getSharedPreferences("user_profile", MODE_PRIVATE);
        String name = prefs.getString("name", "使用者名稱");
        String imageUriStr = prefs.getString("image_uri", null);

        txtUsername.setText(name);

        if (imageUriStr != null) {
            File file = new File(Uri.parse(imageUriStr).getPath());

            Glide.with(this)
                    .load(file)
                    .circleCrop()
                    .placeholder(R.drawable.profile_placeholder)
                    .into(imgProfile);
        } else {
            imgProfile.setImageResource(R.drawable.profile_placeholder);
        }
    }

    // 如果你希望從 EditProfileActivity 返回時刷新畫面：
    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile(); // 回來就重新載入資料
    }
}
