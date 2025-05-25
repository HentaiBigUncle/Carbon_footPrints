package com.example.myapplication2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SetGoalActivity extends AppCompatActivity {
    private EditText editGoal;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_goal);

        editGoal = findViewById(R.id.edit_goal);
        btnSave = findViewById(R.id.btn_save_goal);

        SharedPreferences prefs = getSharedPreferences("user_profile", MODE_PRIVATE);
        double savedGoal = prefs.getFloat("carbon_goal", 0f);
        if (savedGoal > 0) {
            editGoal.setText(String.valueOf(savedGoal));
        }

        btnSave.setOnClickListener(v -> {
            String goalText = editGoal.getText().toString();
            if (!goalText.isEmpty())
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent serviceIntent = new Intent(this, CarbonMonitorService.class);
                    ContextCompat.startForegroundService(this, serviceIntent);
                }
                float goal = Float.parseFloat(goalText);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("carbon_goal", goal);
                editor.apply();
                Toast.makeText(this, "目標已儲存", Toast.LENGTH_SHORT).show();
                finish(); // 返回上一頁
            } else {
                Toast.makeText(this, "請輸入有效目標", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
