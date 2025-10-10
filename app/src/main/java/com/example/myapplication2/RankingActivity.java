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

    String serverIP = Config.SERVER_IP;
    int port = Config.SERVER_PORT;


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
            Socket socket = new Socket(serverIP, port);

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
