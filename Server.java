// java Server11  5000 Hello
import java.net.*;
import java.io.*;
import java.util.*;

public class Server11 {
    private ServerSocket serverSocket;
    private static int port;
    private static String messageout;

    public Server11() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Connected from client: " + socket.getInetAddress().getHostAddress());
                // ✅ 每個客戶開一條 Thread
                new Thread(new ClientHandler(socket, messageout)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Server11 [port] [messageout]");
            System.exit(1);
        }
        port = Integer.parseInt(args[0]);
        messageout = args[1];
        new Server11();
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

            // 回覆客戶端
            out.writeUTF("Server 收到資料，感謝 " + name + "!");
            out.flush();

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }
}
