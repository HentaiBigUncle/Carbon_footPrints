package com.example.myapplication2;

import android.util.Log; // ⚠️ 確保你是 Android 專案才有這個
import java.net.*;
import java.util.*;

public class Config {
    private static final String TAG = "ConfigDebug";

    public static final boolean IS_SERVER = false;
    public static final String SERVER_IP = IS_SERVER ? getLocalIPAddress() : "192.168.127.135"; //TODO:cmd-ipconfig-wifi-ipv4
    public static final int SERVER_PORT = 5000;

    private static String getLocalIPAddress() {
        try {
            Log.d(TAG, "開始搜尋本機 IP...");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Log.d(TAG, "檢查介面：" + iface.getDisplayName());
                Enumeration<InetAddress> addresses = iface.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        Log.d(TAG, "找到 IPv4 位址：" + addr.getHostAddress());
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "取得 IP 位址時發生錯誤：" + e.getMessage(), e);
        }

        Log.w(TAG, "無法找到有效的 IP，預設回傳 127.0.0.1");
        return "127.0.0.1";
    }

    public static void logServerConfig() {
        Log.d(TAG, "=== 伺服器設定 ===");
        Log.d(TAG, "IP: " + SERVER_IP);
        Log.d(TAG, "Port: " + SERVER_PORT);
    }
}
