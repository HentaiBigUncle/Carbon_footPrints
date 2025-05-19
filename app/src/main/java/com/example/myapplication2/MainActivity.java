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

import java.util.*;

public class MainActivity extends AppCompatActivity {

    private TextView videoCarbonView;
    private TextView socialCarbonView;
    private TextView searchCarbonView;
    private TextView totalCarbonView;
    private LineChart carbonChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoCarbonView = findViewById(R.id.videoCarbon);
        socialCarbonView = findViewById(R.id.socialCarbon);
        searchCarbonView = findViewById(R.id.searchCarbon);
        totalCarbonView = findViewById(R.id.carbonText);
        carbonChart = findViewById(R.id.carbonChart);

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

        videoCarbonView.setText(String.format("%.2f kg", video));
        socialCarbonView.setText(String.format("%.2f kg", social));
        searchCarbonView.setText(String.format("%.2f kg", search));
        totalCarbonView.setText(String.format("%.2f kg CO₂", total));
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
        double carbonPerMinute = 0.002;
        for (String key : minutesMap.keySet()) {
            carbonMap.put(key, minutesMap.get(key) * carbonPerMinute);
        }

        return carbonMap;
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
        double carbonPerMinute = 0.002;

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

        LineDataSet dataSet = new LineDataSet(entries, "每小時碳排放 (kg CO₂)");
        dataSet.setColor(ContextCompat.getColor(this, R.color.purple_500));
        dataSet.setValueTextSize(10f);
        dataSet.setCircleRadius(3f);

        LineData lineData = new LineData(dataSet);
        carbonChart.setData(lineData);
        carbonChart.invalidate();
    }
}
