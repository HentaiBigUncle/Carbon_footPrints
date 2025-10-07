package com.example.myapplication2;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.media.AudioAttributes;
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
import android.media.AudioManager;

import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

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
    private TextView screenCarbonView;

    private static final int SERVER_PORT = 5000;

    private List<String> homeLaunchers;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAndRequestPermissions(); // 啟動權限檢查流程
    }



    private void checkAndRequestPermissions() {
        if (!hasUsageStatsPermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }

        // 所有權限已取得，進行主邏輯初始化
        initUIAndLogic();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionsOnResume();

    }
    private boolean requestedUsageAccess = false;
    private boolean requestedNotificationAccess = false;

    private void checkPermissionsOnResume() {
        if (!hasUsageStatsPermission()) {
            if (!requestedUsageAccess) {
                requestedUsageAccess = true;
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            return;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            if (!requestedNotificationAccess) {
                requestedNotificationAccess = true;
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            return;
        }

        // 權限都取得後再做更新
        updateCarbonUI();
        drawHourlyCarbon();
        loadUserProfile();
    }


    private void initUIAndLogic() {
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
        screenCarbonView = findViewById(R.id.ScreenCarbon);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// 關閉勿擾模式
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

// 設定為正常響鈴模式（非靜音／震動）
        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

// 通知音量設定為最大 80%
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        int targetVolume = (int) (maxVolume * 0.8);
//        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, targetVolume, AudioManager.FLAG_SHOW_UI);

// 你也可以視情況設定媒體音量
        int mediaMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(mediaMax * 0.8), AudioManager.FLAG_SHOW_UI);


        // 取得桌面啟動器清單
        homeLaunchers = getHomeLauncherPackages();

        // 啟動提醒邏輯
        startAppReminderLoop();

        // 其他初始化（可自行加入）
        updateCarbonUI();
        setupChart();
        drawHourlyCarbon();

        // 按鈕設定
        Button profileBtn = findViewById(R.id.btn_profile);
        profileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });
    }

    private void updateCarbonUI() {
        Map<String, Double> carbonMap = calculateCarbonFootprintByCategory();

        double video = carbonMap.getOrDefault("video", 0.0);
        double social = carbonMap.getOrDefault("social", 0.0);
        double search = carbonMap.getOrDefault("search", 0.0);


        double screenTotalCarbon = getTotalScreenTimeCarbon();  // 螢幕使用總時間
        double total = screenTotalCarbon + social + search + video;
        videoCarbonView.setText(String.format(Locale.getDefault(), "%.0f g", video));
        socialCarbonView.setText(String.format(Locale.getDefault(), "%.0f g", social));
        searchCarbonView.setText(String.format(Locale.getDefault(), "%.0f g", search));
        screenCarbonView.setText(String.format(Locale.getDefault(), "%.0f g", screenTotalCarbon));
        totalCarbonView.setText(String.format(Locale.getDefault(), "%.0f g CO₂", total));

        sendMessageToServer(total);
        updateTopUsageSeconds();
    }

    private double getTotalScreenTimeCarbon() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        List<UsageStats> statsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

        long totalForegroundTimeMs = 0;
        if (statsList != null) {
            for (UsageStats stat : statsList) {
                totalForegroundTimeMs += stat.getTotalTimeInForeground();
            }
        }

        double carbonPerMinute = 2.0; // 每分鐘碳排放量
        return (totalForegroundTimeMs / 60000.0) * carbonPerMinute;

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

            } else if (pkg.equals("com.instagram.android") || pkg.contains("threads") ||pkg.equals("com.facebook.orca")) {
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
            } else if  (pkg.equals("com.facebook.orca")) { //含messenger
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

    private List<Double> calculateHourlyCarbon() {
        List<Double> hourlyCarbon = new ArrayList<>(Collections.nCopies(24, 0.0));

        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        // 今日 00:00 到現在
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();

        UsageEvents events = usageStatsManager.queryEvents(startTime, endTime);
        UsageEvents.Event event = new UsageEvents.Event();

        Map<String, Long> appForegroundStartMap = new HashMap<>();

        while (events.hasNextEvent()) {
            events.getNextEvent(event);

            if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                appForegroundStartMap.put(event.getPackageName(), event.getTimeStamp());
            } else if (event.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED
                    && appForegroundStartMap.containsKey(event.getPackageName())) {

                long start = appForegroundStartMap.get(event.getPackageName());
                long end = event.getTimeStamp();

                // 分段計算落在哪幾小時
                long durationMs = end - start;

                Calendar startCal = Calendar.getInstance();
                startCal.setTimeInMillis(start);
                int startHour = startCal.get(Calendar.HOUR_OF_DAY);

                Calendar endCal = Calendar.getInstance();
                endCal.setTimeInMillis(end);
                int endHour = endCal.get(Calendar.HOUR_OF_DAY);

                if (startHour == endHour) {
                    hourlyCarbon.set(startHour,
                            hourlyCarbon.get(startHour) + durationMs / 60000.0 * 2.0);
                } else {
                    // 跨小時情況，分割時間到對應小時
                    for (int h = startHour; h <= endHour && h < 24; h++) {
                        Calendar hourStart = (Calendar) startCal.clone();
                        hourStart.set(Calendar.HOUR_OF_DAY, h);
                        hourStart.set(Calendar.MINUTE, 0);
                        hourStart.set(Calendar.SECOND, 0);
                        hourStart.set(Calendar.MILLISECOND, 0);

                        long hourStartMs = hourStart.getTimeInMillis();
                        long hourEndMs = hourStartMs + 3600000L;

                        long overlapStart = Math.max(start, hourStartMs);
                        long overlapEnd = Math.min(end, hourEndMs);
                        long overlapDuration = Math.max(0, overlapEnd - overlapStart);

                        hourlyCarbon.set(h, hourlyCarbon.get(h) + overlapDuration / 60000.0 * 2.0);
                    }
                }

                appForegroundStartMap.remove(event.getPackageName());
            }
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
        put("其他", new ArrayList<>());
    }};
    private List<String> getHomeLauncherPackages() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfoList = getPackageManager().queryIntentActivities(intent, 0);
        List<String> launcherPackages = new ArrayList<>();
        for (ResolveInfo info : resolveInfoList) {
            launcherPackages.add(info.activityInfo.packageName);
        }
        return launcherPackages;
    }

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

                // 先判斷是否是桌面或無前景 App，直接 return
                if (foregroundPkg == null || homeLaunchers.contains(foregroundPkg)) {
                    usageHandler.postDelayed(this, CHECK_INTERVAL_MS);
                    return;
                }

                // 進行分類
                String activeCategory = null;
                for (Map.Entry<String, List<String>> entry : appCategories.entrySet()) {
                    for (String pkg : entry.getValue()) {
                        if (foregroundPkg.equals(pkg)) {
                            activeCategory = entry.getKey();
                            break;
                        }
                    }
                    if (activeCategory != null) break;
                }

                if (activeCategory == null) {
                    activeCategory = "其他";
                    if (!appCategories.containsKey("其他")) {
                        appCategories.put("其他", new ArrayList<>());
                    }
                }

                // 執行提醒邏輯
                for (String category : appCategories.keySet()) {
                    if (category.equals(activeCategory)) {
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
                            if (seconds == 0 && minutes == 0) {
//                                showNotification("使用提醒 - " + category, "你使用的" + category + "會造成大量碳排放，請注意使用時間");
                            } else {
                                String timeStr = (minutes > 0 ? minutes + " 分 " : "") + seconds + " 秒";
//                                showNotification("使用提醒 - " + category, "你已經使用「" + category + "」類 App 超過 " + timeStr);
                            }
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
        String channelName = "YouTube通知頻道";

        // ✅ 建立通知頻道（Android 8.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.warm);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("用來提醒 YouTube 使用狀況的通知");
            channel.setSound(soundUri, audioAttributes); // ✅ 設定聲音

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // ✅ Android 13+ 檢查通知權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                return;
            }
        }

        // ✅ 通知聲音（若是 Android 8.0 以下才用 setSound，否則聲音由頻道控制）
        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.warm);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(soundUri); // ✅ Android 8.0 以下要這樣設聲音
        }

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
    private void sendMessageToServer(double total) {
        new Thread(() -> {
            try {
                String serverIP = "192.168.0.181";  // 換成你的伺服器 IP
                int port = 5000;
                Socket socket = new Socket(serverIP, port);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream());

                // 從 SharedPreferences 取出使用者名稱
                SharedPreferences prefs = getSharedPreferences("user_profile", MODE_PRIVATE);
                String name = prefs.getString("name", "使用者");

                // ✅ 傳送格式：「使用者名稱,total」
                String message = name + "," + total;
                out.writeUTF(message);
                out.flush();

                // 讀取伺服器回覆
                String response = in.readUTF();
                Log.d("Client", "Server response: " + response);

                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                Log.e("Client", "Error connecting to server: " + e.getMessage());
            }
        }).start();
    }



}