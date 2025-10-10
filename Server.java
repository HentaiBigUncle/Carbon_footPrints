/*
 javac -cp ".;lib/json-20231013.jar" Server.java
 java -cp ".;lib/json-20231013.jar" Server 5000 Hello
*/
import java.net.*;
import java.io.*;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class Server {
    private ServerSocket serverSocket;
    private static int port;
    private static String messageout;
    private static final String DATA_FILE = "data.txt";

    public Server() {
        try {
            serverSocket = new ServerSocket(port);
           serverSocket.setReuseAddress(true);

            System.out.println("Server started on IP " +
                InetAddress.getLocalHost().getHostAddress() +
                " and port " + port);


            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Connected from client: " + socket.getInetAddress().getHostAddress());
                new Thread(new ClientHandler(socket, messageout)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Server socket closed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        }
    }   

   public static synchronized void saveData(String name, String total) {
    try {
        // 建立 JSON 資料
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("total", total);

        // 你的 Google Script 部署網址
        String urlString = "https://script.google.com/macros/s/AKfycbwKnCwuT4fwapsoBuXC2NMKPjVdw45eDvODDzePZy2O5mwBHjgGSbHSfL32MBr3rSfarg/exec";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(obj.toString().getBytes("UTF-8"));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("成功上傳至 Google Sheets");
        } else {
            System.out.println("上傳失敗，狀態碼: " + responseCode);
        }

        conn.disconnect();
    } catch (Exception e) {
        System.out.println("Error uploading to Google Sheets: " + e.getMessage());
    }
}


public static synchronized String readAllData() {
    try {
        String urlString = "https://script.google.com/macros/s/AKfycbwKnCwuT4fwapsoBuXC2NMKPjVdw45eDvODDzePZy2O5mwBHjgGSbHSfL32MBr3rSfarg/exec";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();

        return sb.toString();
    } catch (Exception e) {
        return "[]";
    }
}

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Server [port] [messageout]");
            System.exit(1);
        }
        port = Integer.parseInt(args[0]);
        messageout = args[1];
        new Server();
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private String messageout;

    public ClientHandler(Socket socket, String messageout) {
        this.socket = socket;
        this.messageout = messageout;
    }

@Override
public void run() {
    try {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        String message = in.readUTF();

        // ✅ 如果是排行榜請求
        if (message.equals("GET_RANKING")) {
            String allData = Server.readAllData();
            out.writeUTF("=== 所有上傳資料 ===\n" + allData);
            out.flush();
        } else {
            // ✅ 一般上傳資料
            String[] parts = message.split(",");
            String name = parts.length > 0 ? parts[0] : "未知";
            String total = parts.length > 1 ? parts[1] : "N/A";

            System.out.println("[" + new Date() + "] 收到使用者: " + name + "，碳排放量: " + total + " g CO2");

            // 儲存到文字檔
            Server.saveData(name, total);

            // 回覆全部資料
            String allData = Server.readAllData();
            out.writeUTF("伺服器已收到資料！\n\n=== 所有上傳資料 ===\n" + allData);
            out.flush();
        }

        in.close();
        out.close();
        socket.close();
    } catch (IOException e) {
        System.out.println("Client disconnected: " + e.getMessage());
    }
}

}
