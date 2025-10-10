package com.example.myapplication2;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class RankingActivity extends AppCompatActivity {
    private TextView textRanking;
    private static final String SERVER_IP = "192.168.6.103"; // 換成你的伺服器 IP
    private static final int SERVER_PORT = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        textRanking = findViewById(R.id.text_ranking);

        // 啟動背景執行緒向伺服器請求資料
        new Thread(this::fetchRankingFromServer).start();
    }

    private void fetchRankingFromServer() {
        try {
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // ✅ 特殊請求，表示只是要排行榜（不上傳資料）
            out.writeUTF("GET_RANKING");
            out.flush();

            // 伺服器回傳的所有資料
            String response = in.readUTF();
            Log.d("Ranking", "Server response:\n" + response);

            // 更新 UI
            runOnUiThread(() -> textRanking.setText(response));

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            Log.e("Ranking", "Error connecting to server: " + e.getMessage());
            runOnUiThread(() -> textRanking.setText("無法連線到伺服器"));
        }
    }
}
