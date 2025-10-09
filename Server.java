// java Server 5000 Hello
import java.net.*;
import java.io.*;
import java.util.*;

public class Server {
    private ServerSocket serverSocket;
    private static int port;
    private static String messageout;
    private static final String DATA_FILE = "data.txt"; // 存放所有上傳資料的檔案

    public Server() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Connected from client: " + socket.getInetAddress().getHostAddress());
                new Thread(new ClientHandler(socket, messageout)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ✅ synchronized：確保多執行緒同時寫入檔案時不會互相干擾
    public static synchronized void saveData(String name, String total) {
        try (FileWriter fw = new FileWriter(DATA_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println(new Date() + " | " + name + " | " + total + " g CO₂");
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }

    // ✅ 讀取整個 data.txt 內容
    public static synchronized String readAllData() {
        StringBuilder sb = new StringBuilder();
        File file = new File(DATA_FILE);

        if (!file.exists()) {
            return "目前尚無任何上傳資料。";
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            return "讀取檔案時發生錯誤：" + e.getMessage();
        }

        return sb.toString();
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

            // 讀取 Android 傳來的「name,total」
            String message = in.readUTF();
            String[] parts = message.split(",");
            String name = parts.length > 0 ? parts[0] : "未知";
            String total = parts.length > 1 ? parts[1] : "N/A";

            System.out.println("[" + new Date() + "] 收到使用者: " + name + "，碳排放量: " + total + " g CO₂");

            // ✅ 儲存到文字檔中
            Server.saveData(name, total);

            // ✅ 回覆：顯示所有資料
            String allData = Server.readAllData();
            out.writeUTF("伺服器已收到資料！\n\n=== 所有上傳資料 ===\n" + allData);
            out.flush();

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }
}
