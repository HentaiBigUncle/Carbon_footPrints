package com.example.myapplication2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RankingActivity extends AppCompatActivity {
    private TableLayout tableRanking;
    String serverIP = Config.SERVER_IP;
    int port = Config.SERVER_PORT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);
        tableRanking = findViewById(R.id.table_ranking);
        new Thread(this::fetchRankingFromServer).start();
    }

    private void fetchRankingFromServer() {
        try {
            Socket socket = new Socket(serverIP, port);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // ✅ 傳送排行榜請求
            out.writeUTF("GET_RANKING");
            out.flush();

            // ✅ 接收伺服器回傳資料
            String response = in.readUTF();
            Log.d("Ranking", "Server response:\n" + response);

            // ✅ 提取 JSON 陣列部分
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            String jsonPart = (start >= 0 && end >= 0) ? response.substring(start, end + 1) : "[]";

            JSONArray arr = new JSONArray(jsonPart);
            List<UserData> list = new ArrayList<>();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.optString("name", "未知");//使用者名稱
                String total = obj.optString("total", "0");//總碳排
                String avatar = obj.optString("avatar", ""); // 新增頭像
                double totalValue = 0;
                try { totalValue = Double.parseDouble(total); } catch (Exception ignore) {}
                list.add(new UserData(name, totalValue, avatar));
            }

            // ✅ 依照 total 排序（數值越小名次越前）
            list.sort(Comparator.comparingDouble(u -> u.total));

            // ✅ 更新 UI
            runOnUiThread(() -> showRankingTable(list));

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            Log.e("Ranking", "Error: " + e.getMessage());
            runOnUiThread(() -> {
                tableRanking.removeAllViews();
                TextView tv = new TextView(this);
                tv.setText("❌ 無法連線到伺服器");
                tableRanking.addView(tv);
            });
        } catch (Exception e) {
            Log.e("Ranking", "Parse error: " + e.getMessage());
        }
    }

    private void showRankingTable(List<UserData> list) {
        tableRanking.removeAllViews();

        // 🏁 表頭
        TableRow header = new TableRow(this);
        header.addView(createCell("名次", true));
        header.addView(createCell("使用者", true));
        header.addView(createCell("碳排放量 (g CO₂)", true));
        tableRanking.addView(header);

        int rank = 1;
        for (UserData user : list) {
            // ✅ 第一行：名次 + 名稱 + 排放量
            TableRow infoRow = new TableRow(this);
            infoRow.addView(createCell(String.valueOf(rank), false));
            infoRow.addView(createCell(user.name, false));
            infoRow.addView(createCell(String.format("%.2f", user.total), false));
            tableRanking.addView(infoRow);

            // ✅ 第二行：頭像（跨三欄置中）
            TableRow avatarRow = new TableRow(this);
            ImageView avatarView = new ImageView(this);
            avatarView.setPadding(8, 8, 8, 8);

            // 嘗試從內部儲存載入使用者頭像
            //✅ 如果你儲存檔案時用使用者名稱命名，這裡可以正確載入。
            //⚠️ 但要注意使用者名稱中不能有特殊字元（如 /, \, ? 等），否則檔案路徑會出錯。

            //可以做一個簡單的安全轉換：
            String safeName = user.name.replaceAll("[^a-zA-Z0-9]", "_");
            File imgFile = new File(getFilesDir(), "profile_images/" + safeName + ".jpg");

            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                avatarView.setImageBitmap(bitmap);
            } else {
                avatarView.setImageResource(R.drawable.profile_placeholder); // 預設頭像
            }

            // 設定頭像大小與跨欄屬性
            TableRow.LayoutParams params = new TableRow.LayoutParams(250, 250);
            params.span = 3; // 橫跨三欄（名次、名稱、排放量）
            params.gravity = Gravity.CENTER;
            avatarView.setLayoutParams(params);

            avatarRow.addView(avatarView);
            tableRanking.addView(avatarRow);

            rank++;
        }
    }

    private TextView createCell(String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(16, 8, 16, 8);
        tv.setTextSize(isHeader ? 18 : 16);
        tv.setTextColor(isHeader ? 0xFF000000 : 0xFF333333);
        tv.setBackgroundColor(isHeader ? 0xFFE0E0E0 : 0xFFFFFFFF);
        return tv;
    }

    // 資料類別
    static class UserData {
        String name;
        double total;
        String avatarPath; // 新增頭像路徑或網址

        UserData(String n, double t, String a) {
            name = n;
            total = t;
            avatarPath = a;
        }
    }
}
