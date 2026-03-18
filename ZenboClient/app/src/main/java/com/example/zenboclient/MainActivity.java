package com.example.zenboclient;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private final ZenboApi api = new ZenboApi();

    private EditText etIp, etSpeakText;
    private TextView tvLog;
    private ScrollView scrollLog;
    private Button btnFollow;
    private Button btnSpin;
    private boolean isFollowing = false;
    private boolean isSpinning  = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etIp        = findViewById(R.id.etIp);
        etSpeakText = findViewById(R.id.etSpeakText);
        tvLog       = findViewById(R.id.tvLog);
        scrollLog   = findViewById(R.id.scrollLog);
        btnFollow   = findViewById(R.id.btnFollow);
        btnSpin     = findViewById(R.id.btnSpin);

        setupButtons();
    }

    private void setupButtons() {

        // 連線
        findViewById(R.id.btnConnect).setOnClickListener(v -> {
            String ip = etIp.getText().toString().trim();
            if (ip.isEmpty()) { toast("請輸入 IP"); return; }
            api.setIp(ip);
            api.ping((ok, msg) -> log(ok ? "連線成功 ✓" : "Ping 失敗，請確認 IP"));
        });

        // 說話
        findViewById(R.id.btnSpeak).setOnClickListener(v -> {
            String text = etSpeakText.getText().toString().trim();
            if (text.isEmpty()) { toast("請輸入說話內容"); return; }
            send(() -> api.speak(text, (ok, msg) -> log("說話：" + (ok ? text : "失敗"))));
        });

        // 表情
        setExprBtn(R.id.btnHappy,     "happy");
        setExprBtn(R.id.btnConfident, "confident");
        setExprBtn(R.id.btnWorried,   "worried");
        setExprBtn(R.id.btnDefault,   "default");

        // 轉圈（toggle）
        btnSpin.setOnClickListener(v -> {
            if (!isSpinning) {
                send(() -> api.spin((ok, msg) -> {
                    if (ok) { isSpinning = true; updateSpinBtn(); }
                    log("轉圈：" + (ok ? "開始" : "失敗"));
                }));
            } else {
                send(() -> api.stop((ok, msg) -> {
                    if (ok) { isSpinning = false; updateSpinBtn(); }
                    log("轉圈：" + (ok ? "停止" : "失敗"));
                }));
            }
        });

        // Follow Me
        btnFollow.setOnClickListener(v -> {
            if (!isFollowing) {
                send(() -> api.startFollow((ok, msg) -> {
                    if (ok) { isFollowing = true; updateFollowBtn(); }
                    log("Follow Me：" + (ok ? "開啟" : "失敗"));
                }));
            } else {
                send(() -> api.stopFollow((ok, msg) -> {
                    if (ok) { isFollowing = false; updateFollowBtn(); }
                    log("Follow Me：" + (ok ? "關閉" : "失敗"));
                }));
            }
        });

        // 輪燈 - 顏色
        setLightBtn(R.id.btnLightRed,    "red",    "solid");
        setLightBtn(R.id.btnLightGreen,  "green",  "solid");
        setLightBtn(R.id.btnLightBlue,   "blue",   "solid");
        setLightBtn(R.id.btnLightYellow, "yellow", "solid");
        setLightBtn(R.id.btnLightPurple, "purple", "solid");
        setLightBtn(R.id.btnLightCyan,   "cyan",   "solid");
        setLightBtn(R.id.btnLightOrange, "orange", "solid");
        setLightBtn(R.id.btnLightWhite,  "white",  "solid");

        // 輪燈 - 效果
        setLightBtn(R.id.btnLightBlink,   "white", "blink");
        setLightBtn(R.id.btnLightBreathe, "white", "breathe");
        setLightBtn(R.id.btnLightMarquee, "white", "marquee");
        setLightBtn(R.id.btnLightOff,     "off",   "off");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setExprBtn(int id, String face) {
        findViewById(id).setOnClickListener(v ->
                send(() -> api.setExpression(face, (ok, msg) ->
                        log("表情 " + face + "：" + (ok ? "OK" : "失敗")))));
    }

    private void setLightBtn(int id, String color, String mode) {
        findViewById(id).setOnClickListener(v ->
                send(() -> api.setLight(color, mode, (ok, msg) ->
                        log("輪燈 " + color + " " + mode + "：" + (ok ? "OK" : "失敗")))));
    }

    private void send(Runnable r) {
        if (!api.isConfigured()) { toast("請先連線"); return; }
        r.run();
    }

    private void updateSpinBtn() {
        btnSpin.setText(isSpinning ? "停止轉圈" : "轉一圈");
        btnSpin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                isSpinning ? 0xFFE53935 : 0xFF5C6BC0));
    }

    private void updateFollowBtn() {
        btnFollow.setText(isFollowing ? "停止 Follow Me" : "Follow Me");
        btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                isFollowing ? 0xFFE53935 : 0xFF4CAF50));
    }

    private void log(String msg) {
        String cur = tvLog.getText().toString();
        String[] lines = cur.split("\n");
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, lines.length - 59);
        for (int i = start; i < lines.length; i++) sb.append(lines[i]).append("\n");
        sb.append("> ").append(msg);
        tvLog.setText(sb.toString());
        scrollLog.post(() -> scrollLog.fullScroll(View.FOCUS_DOWN));
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
