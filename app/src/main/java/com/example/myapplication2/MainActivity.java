package com.example.myapplication2;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import java.io.File;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import androidx.core.content.ContextCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Handler;
import androidx.core.app.NotificationCompat;
import java.util.*;
import android.app.usage.UsageEvents;
import android.os.Build;
import android.Manifest;
import androidx.core.app.NotificationManagerCompat;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity
{
    private TextView rank1View;
    private TextView rank2View;
    private TextView rank3View;
    private TextView resultText;
    private TextView rank4View;
    private TextView rank5View;
    private ImageView imgProfile;
    private TextView txtUsername;
    private TextView videoCarbonView;
    private TextView socialCarbonView;
    private TextView searchCarbonView;
    private TextView totalCarbonView;
    private LineChart carbonChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rank1View = findViewById(R.id.rank1);
        rank2View = findViewById(R.id.rank2);
        rank3View = findViewById(R.id.rank3);
        rank4View = findViewById(R.id.rank4);
        rank5View = findViewById(R.id.rank5);
        imgProfile = findViewById(R.id.img_profile);
        txtUsername = findViewById(R.id.profile_name);
        videoCarbonView = findViewById(R.id.videoCarbon);
        socialCarbonView = findViewById(R.id.socialCarbon);
        searchCarbonView = findViewById(R.id.searchCarbon);
        totalCarbonView = findViewById(R.id.carbonText);
        carbonChart = findViewById(R.id.carbonChart);
        if (!hasUsageStatsPermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);

        } else {
            startAppReminderLoop();
            showNotification("通知測試", "如果你看到這個，就表示通知可以正常運作");
        }
        //loadUserProfile();
        updateCarbonUI();
        setupChart();
        drawHourlyCarbon();
        Button profileBtn = findViewById(R.id.btn_profile);
        profileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });
        startAppReminderLoop();
    }
    private void updateCarbonUI() {
        Map<String, Double> carbonMap = calculateCarbonFootprintByCategory();

        double video = carbonMap.getOrDefault("video", 0.0);
        double social = carbonMap.getOrDefault("social", 0.0);
        double search = carbonMap.getOrDefault("search", 0.0);
        double total = video + social + search;

        videoCarbonView.setText(String.format(Locale.getDefault(), "%.0f g", video));
        socialCarbonView.setText(String.format(Locale.getDefault(), "%.0f g", social));
        searchCarbonView.setText(String.format(Locale.getDefault(), "%.0f g", search));
        totalCarbonView.setText(String.format(Locale.getDefault(), "%.0f g CO₂", total));
        updateTopUsageSeconds();

    }

    private Map<String, Double> calculateCarbonFootprintByCategory() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        Calendar end = Calendar.getInstance();

        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, start.getTimeInMillis(), end.getTimeInMillis());

        Map<String, Long> minutesMap = new HashMap<>();

        for (UsageStats stats : usageStatsList) {
            String pkg = stats.getPackageName();
            long minutes = stats.getTotalTimeInForeground() / 60000;

            if (pkg.equals("com.google.android.youtube")|| pkg.equals("com.netflix.mediaclient") || pkg.equals("com.google.android.apps.youtube.kids"))
            {
                minutesMap.put("video", minutesMap.getOrDefault("video", 0L) + minutes);

            } else if (pkg.equals("com.instagram.android") || pkg.contains("threads")) {
                minutesMap.put("social", minutesMap.getOrDefault("social", 0L) + minutes);
            } else if (pkg.contains("chrome") || pkg.contains("browser")) {
                minutesMap.put("search", minutesMap.getOrDefault("search", 0L) + minutes);
            }
        }

        Map<String, Double> carbonMap = new HashMap<>();
        double carbonPerMinute = 2.0;  // 單位為公克（原本是 0.002 kg）
        for (String key : minutesMap.keySet()) {
            carbonMap.put(key, minutesMap.get(key) * carbonPerMinute);
        }

        return carbonMap;
    }
    private Map<String, Long> calculateUsageSecondsByCategory() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        Calendar end = Calendar.getInstance();

        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, start.getTimeInMillis(), end.getTimeInMillis());

        Map<String, Long> secondsMap = new HashMap<>();

        for (UsageStats stats : usageStatsList) {
            String pkg = stats.getPackageName();
            long seconds = stats.getTotalTimeInForeground() / 1000;

            if (pkg.equals("com.google.android.youtube") || pkg.equals("com.netflix.mediaclient") || pkg.equals("com.google.android.apps.youtube.kids")) {
                secondsMap.put("影音", secondsMap.getOrDefault("影音", 0L) + seconds);
            } else if  (pkg.equals("com.facebook.orca")) { //messenger
                secondsMap.put("社群", secondsMap.getOrDefault("社群", 0L) + seconds);
            } else if (pkg.contains("chrome") || pkg.contains("browser")) {
                secondsMap.put("搜尋", secondsMap.getOrDefault("搜尋", 0L) + seconds);
            }
            else if (pkg.contains("com.google.android.apps.maps") ) {
                secondsMap.put("地圖", secondsMap.getOrDefault("地圖", 0L) + seconds);
            }
            else if (pkg.contains("com.google.android.gm") ) {
                secondsMap.put("郵件", secondsMap.getOrDefault("郵件", 0L) + seconds);
            }
        }

        return secondsMap;
    }

    private void updateTopUsageSeconds() {
        Map<String, Long> secondsMap = calculateUsageSecondsByCategory();

        List<Map.Entry<String, Long>> sortedList = new ArrayList<>(secondsMap.entrySet());
        sortedList.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));  // 由大到小排序

        TextView[] rankViews = { rank1View, rank2View, rank3View,rank4View,rank5View };

        for (int i = 0; i < rankViews.length; i++) {
            if (i < sortedList.size()) {
                Map.Entry<String, Long> entry = sortedList.get(i);
                rankViews[i].setText(String.format(Locale.getDefault(), "第%d名: %s(%d 秒)", i+1,entry.getKey(), entry.getValue()));
            } else {
                rankViews[i].setText("");
            }
        }
    }
    private Map<Integer, Long> getHourlyForegroundUsage() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);

        Calendar end = Calendar.getInstance();

        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, start.getTimeInMillis(), end.getTimeInMillis());

        Map<Integer, Long> hourlyUsageMap = new HashMap<>();

        for (UsageStats stats : usageStatsList) {
            long totalTime = stats.getTotalTimeInForeground();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(stats.getLastTimeUsed());
            int hour = c.get(Calendar.HOUR_OF_DAY);
            hourlyUsageMap.put(hour, hourlyUsageMap.getOrDefault(hour, 0L) + totalTime);
        }

        return hourlyUsageMap;
    }

    private List<Double> calculateHourlyCarbon() {
        Map<Integer, Long> usage = getHourlyForegroundUsage();
        double carbonPerMinute = 2.0;  // 公克

        List<Double> hourlyCarbon = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            long timeMs = usage.getOrDefault(i, 0L);
            double carbon = (timeMs / 60000.0) * carbonPerMinute;
            hourlyCarbon.add(carbon);
        }
        return hourlyCarbon;
    }

    private void setupChart() {
        carbonChart.getDescription().setEnabled(false);
        carbonChart.setDrawGridBackground(false);

        XAxis xAxis = carbonChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(24);
        xAxis.setDrawGridLines(false);

        carbonChart.getAxisRight().setEnabled(false);
    }

    private void drawHourlyCarbon() {
        List<Double> hourlyCarbon = calculateHourlyCarbon();
        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < hourlyCarbon.size(); i++) {
            entries.add(new Entry(i, hourlyCarbon.get(i).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "每小時碳排放 (g CO₂)");
        dataSet.setColor(ContextCompat.getColor(this, R.color.purple_500));
        dataSet.setValueTextSize(10f);
        dataSet.setCircleRadius(3f);

        LineData lineData = new LineData(dataSet);
        carbonChart.setData(lineData);
        carbonChart.invalidate();
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
    // 分類對應的 app packages（可自由擴充）
    private final Map<String, List<String>> appCategories = new HashMap<String, List<String>>() {{
        put("影音", Arrays.asList("com.google.android.youtube", "com.netflix.mediaclient", "com.google.android.apps.youtube.kids"));
        put("社群", Arrays.asList("com.facebook.orca"));
        put("搜尋", Arrays.asList("com.android.chrome", "org.mozilla.firefox", "com.android.browser"));
        put("地圖", Arrays.asList("com.google.android.apps.maps"));
        put("郵件", Arrays.asList("com.google.android.gm"));
    }};

    // 記錄各分類啟動時間與最後通知秒數
    private final Map<String, Long> categoryStartTime = new HashMap<>();
    private final Map<String, Integer> lastNotifiedSecondsMap = new HashMap<>();
    private final Handler usageHandler = new Handler();
    private final long CHECK_INTERVAL_MS = 1000;

    private void startAppReminderLoop() {
        usageHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                String foregroundPkg = getForegroundApp();

                // 判斷前景 app 屬於哪個分類
                String activeCategory = null;
                for (Map.Entry<String, List<String>> entry : appCategories.entrySet()) {
                    for (String pkg : entry.getValue()) {
                        if (foregroundPkg != null && foregroundPkg.contains(pkg)) {
                            activeCategory = entry.getKey();
                            break;
                        }
                    }
                    if (activeCategory != null) break;
                }

                // 處理提醒邏輯
                for (String category : appCategories.keySet()) {
                    if (category.equals(activeCategory)) {
                        // 該分類 app 在前景
                        long startTime = categoryStartTime.getOrDefault(category, 0L);
                        if (startTime == 0) {
                            categoryStartTime.put(category, now); // 初始化開始時間
                            lastNotifiedSecondsMap.put(category, -1);
                        }

                        long elapsed = now - categoryStartTime.get(category);
                        int elapsedSeconds = (int) (elapsed / 1000);

                        if (elapsedSeconds % 5 == 0 && elapsedSeconds != lastNotifiedSecondsMap.getOrDefault(category, -1)) {
                            lastNotifiedSecondsMap.put(category, elapsedSeconds);
                            int minutes = elapsedSeconds / 60;
                            int seconds = elapsedSeconds % 60;
                            String timeStr = (minutes > 0 ? minutes + " 分 " : "") + seconds + " 秒";

                            showNotification("使用提醒 - " + category, "你已經使用「" + category + "」類 App 超過 " + timeStr);
                        }

                    } else {
                        // 不在前景，重置
                        categoryStartTime.put(category, 0L);
                        lastNotifiedSecondsMap.put(category, -1);
                    }
                }

                usageHandler.postDelayed(this, CHECK_INTERVAL_MS);
            }
        }, CHECK_INTERVAL_MS);
    }

    private String getForegroundApp() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        long now = System.currentTimeMillis();
        long beginTime = now - 60 * 1000;
        UsageEvents events = usageStatsManager.queryEvents(beginTime, now);

        UsageEvents.Event event = new UsageEvents.Event();
        String lastForegroundApp = null;
        long lastTimestamp = 0;

        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (event.getTimeStamp() > lastTimestamp) {
                    lastTimestamp = event.getTimeStamp();
                    lastForegroundApp = event.getPackageName();
                }
            }
        }

        return lastForegroundApp;
    }


    private void showNotification(String title, String message) {
        String channelId = "youtube_alert_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(1001, builder.build());
    }
    private boolean hasUsageStatsPermission() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);

        if (stats == null || stats.isEmpty()) {
            return false;
        }

        // 進一步檢查 AppOpsManager 狀態
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), getPackageName());
        } else {
            mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), getPackageName());
        }

        return mode == AppOpsManager.MODE_ALLOWED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCarbonUI();
        drawHourlyCarbon();
        loadUserProfile();
    }
}