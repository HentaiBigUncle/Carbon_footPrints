package com.example.myapplication2;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.*;
import android.app.usage.UsageEvents;
import android.os.Build;
import android.Manifest;
import androidx.core.app.NotificationManagerCompat;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private TextView videoCarbonView;
    private TextView socialCarbonView;
    private TextView searchCarbonView;
    private TextView totalCarbonView;
    private LineChart carbonChart;
    private TextView rank1View;
    private TextView rank2View;
    private TextView rank3View;
    private TextView resultText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoCarbonView = findViewById(R.id.videoCarbon);
        socialCarbonView = findViewById(R.id.socialCarbon);
        searchCarbonView = findViewById(R.id.searchCarbon);
        totalCarbonView = findViewById(R.id.carbonText);
        carbonChart = findViewById(R.id.carbonChart);
        rank1View = findViewById(R.id.rank1);
        rank2View = findViewById(R.id.rank2);
        rank3View = findViewById(R.id.rank3);

        if (!hasUsageStatsPermission2()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
            resultText.setText("請開啟『使用狀況存取』權限後回來重新啟動 App");
        } else {
            startYoutubeReminderLoop();
            showNotification("通知測試", "如果你看到這個，就表示通知可以正常運作");
        }
        if (!hasUsageStatsPermission()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }

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
        startYoutubeReminderLoop();


    }
    @Override
    protected void onResume() {
        super.onResume();
        updateCarbonUI();
        drawHourlyCarbon();
    }

    private boolean hasUsageStatsPermission() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
        updateTop3UsageSeconds();

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
            } else if  (pkg.equals("com.facebook.katana")) {
                secondsMap.put("社群", secondsMap.getOrDefault("社群", 0L) + seconds);
            } else if (pkg.contains("chrome") || pkg.contains("browser")) {
                secondsMap.put("搜尋", secondsMap.getOrDefault("搜尋", 0L) + seconds);
            }
        }

        return secondsMap;
    }

    private void updateTop3UsageSeconds() {
        Map<String, Long> secondsMap = calculateUsageSecondsByCategory();

        List<Map.Entry<String, Long>> sortedList = new ArrayList<>(secondsMap.entrySet());
        sortedList.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));  // 由大到小排序

        TextView[] rankViews = { rank1View, rank2View, rank3View };

        for (int i = 0; i < rankViews.length; i++) {
            if (i < sortedList.size()) {
                Map.Entry<String, Long> entry = sortedList.get(i);
                rankViews[i].setText(String.format(Locale.getDefault(), "%s：%d 秒", entry.getKey(), entry.getValue()));
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
    private final Handler youtubeHandler = new Handler();
    private final String YOUTUBE_PACKAGE = "com.google.android.youtube";
    private final long CHECK_INTERVAL_MS = 1000; // 每 1 秒檢查一次（更精細）

    private long totalUsageTimeMs = 0; // 累積的前景時間

    private void startYoutubeReminderLoop() {
        youtubeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAppInForeground(YOUTUBE_PACKAGE)) {
                    totalUsageTimeMs += CHECK_INTERVAL_MS;

                    // 每 5 秒提醒一次
                    if (totalUsageTimeMs % (5 * 1000) == 0) {
                        long totalSeconds = totalUsageTimeMs / 1000;
                        long minutes = totalSeconds / 60;
                        long seconds = totalSeconds % 60;
                        String timeString = (minutes > 0 ? minutes + " 分 " : "") + seconds + " 秒";
                        showNotification("YouTube 使用提醒", "你已經使用 YouTube 超過 " + timeString + " 了");
                    }
                } else {
                    // 若離開 YouTube 就重設時間
                    totalUsageTimeMs = 0;
                }

                youtubeHandler.postDelayed(this, CHECK_INTERVAL_MS);
            }
        }, CHECK_INTERVAL_MS);
    }




    private boolean isAppInForeground(String packageName) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        long now = System.currentTimeMillis();
        long beginTime = now - 60 * 1000; // 查詢最近 1 分鐘內的事件
        UsageEvents usageEvents = usageStatsManager.queryEvents(beginTime, now);

        UsageEvents.Event event = new UsageEvents.Event();
        String lastForegroundApp = null;
        long lastTimestamp = 0;

        // 找出最後一個 MOVE_TO_FOREGROUND 事件的 app
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (event.getTimeStamp() > lastTimestamp) {
                    lastTimestamp = event.getTimeStamp();
                    lastForegroundApp = event.getPackageName();
                }
            }
        }

        return packageName.equals(lastForegroundApp);
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

    private boolean hasUsageStatsPermission2() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }





}
