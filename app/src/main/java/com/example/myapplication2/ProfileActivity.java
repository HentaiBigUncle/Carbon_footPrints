package com.example.myapplication2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.app.usage.UsageStatsManager;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import java.io.File;
import com.bumptech.glide.Glide;
import java.util.*;
import android.app.usage.UsageStats;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class ProfileActivity extends AppCompatActivity {
    private ProgressBar progressGoal;
    private TextView txtGoalProgress;
    private ImageView imgProfile;
    private TextView txtUsername;
    private TextView carbonTextView;
    private Handler handler;
    private Runnable updateCarbonRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); // 你自己的 XML 檔名

        handler = new Handler(Looper.getMainLooper());
        imgProfile = findViewById(R.id.img_profile);       // 對應你的 XML 中的 ID
        txtUsername = findViewById(R.id.txt_username);
        carbonTextView = findViewById(R.id.txt_today_carbon);

        Button profileBtn = findViewById(R.id.btn_edit_profile);
        profileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                startActivity(intent);
            }
        });
        Button btnSetGoal = findViewById(R.id.btn_set_goal);
        btnSetGoal.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, SetGoalActivity.class);
            startActivity(intent);
        });
        checkNotificationPermission();

        startCarbonUpdater();
    }

    private void loadUserProfile() {
        SharedPreferences prefs = getSharedPreferences("user_profile", MODE_PRIVATE);
        String name = prefs.getString("name", "使用者名稱");

        txtUsername.setText(name);

        // 建立檔案路徑
        File directory = new File(getFilesDir(), "profile_images");
        String safeName = name.replaceAll("[^a-zA-Z0-9]", "_");
        File imgFile = new File(getFilesDir(), "profile_images/" + safeName + ".jpg");

        if (imgFile.exists()) {
            Glide.with(this)
                    .load(imgFile)
                    .circleCrop()
                    .placeholder(R.drawable.profile_placeholder)
                    .into(imgProfile);
        } else {
            imgProfile.setImageResource(R.drawable.profile_placeholder);
        }
    }


    private void updateGoalProgress() {
        progressGoal = findViewById(R.id.progress_goal);
        txtGoalProgress = findViewById(R.id.txt_goal_progress);

        SharedPreferences prefs = getSharedPreferences("user_profile", MODE_PRIVATE);
        float goal = prefs.getFloat("carbon_goal", 0f);

        // 假設你有計算今日已排放的碳（例如透過 calculateTodayCarbon()）
        float todayCarbon = getTodayCarbon();

        if (goal > 0)
        {
            int progress = (int) ((todayCarbon / goal) * 100);
            if (progress > 100) progress = 100;

            progressGoal.setProgress(progress);
            txtGoalProgress.setText("今日使用量：" + progress + "%");
        }
        else
        {
            txtGoalProgress.setText("尚未設定使用上限");
            progressGoal.setProgress(0);
        }

    }
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }
    private float getTodayCarbon()
    {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        // 計算今天開始的時間戳（00:00）
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        // 查詢今日的 App 使用統計
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

        long totalForegroundTimeMs = 0;

        if (usageStatsList != null) {
            for (UsageStats usageStats : usageStatsList) {
                long timeInForeground = usageStats.getTotalTimeInForeground();
                totalForegroundTimeMs += timeInForeground;
            }
        }

        // 碳排係數：每分鐘使用時間對應的碳排放量（單位：g CO₂）
        double carbonPerMinute = 2.0;

        // 換算成分鐘後乘上係數
        return (float) ((totalForegroundTimeMs / 60000.0) * carbonPerMinute);
    }
    private void startCarbonUpdater() {
        updateCarbonRunnable = new Runnable() {
            @Override
            public void run() {
                float carbon = getTodayCarbon();
                String carbonText = String.format("今日碳足跡：%.2f g CO₂", carbon);
                carbonTextView.setText(carbonText);

                handler.postDelayed(this, 1000); // 每 1 秒更新一次
            }
        };
        handler.post(updateCarbonRunnable);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateCarbonRunnable); // 避免 memory leak
    }
    // 如果你希望從 EditProfileActivity 返回時刷新畫面：
    @Override
    protected void onResume()
    {
        super.onResume();
        loadUserProfile(); // 回來就重新載入資料
        updateGoalProgress();
    }
}
