package com.example.zenboclient;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZenboApi {

    private static final String TAG = "ZenboApi";
    private static final int TIMEOUT_MS = 3000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String baseUrl;

    public interface Callback {
        void onResult(boolean success, String message);
    }

    public void setIp(String ip) { this.baseUrl = "http://" + ip.trim() + ":8080"; }
    public boolean isConfigured() { return baseUrl != null; }

    public void ping(Callback cb)                          { get("/ping", cb); }
    public void speak(String text, Callback cb)            { get("/speak?text=" + encode(text), cb); }
    public void setExpression(String face, Callback cb)    { get("/expression?face=" + face, cb); }
    public void spin(Callback cb)                          { get("/spin", cb); }
    public void stop(Callback cb)                          { get("/stop", cb); }
    public void startFollow(Callback cb)                   { get("/follow?enable=true", cb); }
    public void stopFollow(Callback cb)                    { get("/follow?enable=false", cb); }

    /** 輪燈。color: red/green/blue/yellow/white/purple/cyan/orange/off */
    public void setLight(String color, String mode, Callback cb) {
        get("/light?color=" + color + "&mode=" + mode, cb);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void get(String path, Callback cb) {
        if (baseUrl == null) { if (cb != null) cb.onResult(false, "請先設定 IP"); return; }
        String url = baseUrl + path;
        executor.execute(() -> {
            String result = httpGet(url);
            boolean ok = result != null;
            if (cb != null) mainHandler.post(() -> cb.onResult(ok, ok ? result : "連線失敗"));
        });
    }

    private String httpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.connect();
            if (conn.getResponseCode() == 200)
                return new String(conn.getInputStream().readAllBytes());
            return null;
        } catch (IOException e) {
            Log.e(TAG, "HTTP error: " + urlStr, e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String encode(String text) {
        try { return java.net.URLEncoder.encode(text, "UTF-8"); }
        catch (Exception e) { return text; }
    }
}
