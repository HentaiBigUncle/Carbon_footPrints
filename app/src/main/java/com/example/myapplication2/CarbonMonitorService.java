package com.example.myapplication2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CarbonMonitorService extends Service {
    private Timer timer;
    private double dailyGoal = 100.0; // g CO₂（可動態設定）

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        SharedPreferences prefs = getSharedPreferences("user_profile", MODE_PRIVATE);
        dailyGoal = prefs.getFloat("carbon_goal", 100.0f);
        Notification notification = new NotificationCompat.Builder(this, "carbon_channel")
                .setContentTitle("碳足跡監測中")
                .setContentText("持續監測使用時間...")
                .setSmallIcon(R.drawable.ic_baseline_monitor)
                .build();

        startForeground(1, notification);

        startMonitoring(); // 啟動定時監測
        return START_STICKY;
    }

    private void startMonitoring() {
        timer = new Timer();
        scheduleNextCheck();
    }

    private void scheduleNextCheck() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                double carbon = getTodayCarbon();
                if (carbon >= dailyGoal) {
                    showOveruseNotification();
                    stopSelf();
                } else {
                    // 安全排程下一次
                    scheduleNextCheck();
                }
            }
        }, 3 * 1000); // 每3秒排一次
    }


    private double getTodayCarbon() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

        long totalForegroundTimeMs = 0;
        for (UsageStats usageStats : usageStatsList) {
            totalForegroundTimeMs += usageStats.getTotalTimeInForeground();
        }

        return (totalForegroundTimeMs / 60000.0) * 2.0; // 每分鐘 2g CO₂
    }

    private void showOveruseNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "carbon_channel")
                .setSmallIcon(R.drawable.ic_baseline_warning)
                .setContentTitle("已達減碳目標！")
                .setContentText("今日已達使用上限，請暫停使用手機。")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat manager = NotificationManagerCompat.from(this);
            manager.notify(1001, builder.build());
        } catch (SecurityException e) {
            Log.e("CarbonMonitor", "未授權發送通知", e);
        }
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "carbon_channel",
                    "碳監測通知",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
