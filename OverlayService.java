package com.example.punimenu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Button;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.pm.ServiceInfo;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private View bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;
    private SharedPreferences prefs;
    private static final String CHANNEL_ID = "punimenu_overlay";

    private final int bg = Color.rgb(22, 17, 34);
    private final int card = Color.rgb(38, 27, 55);
    private final int purple = Color.rgb(177, 112, 255);
    private final int white = Color.WHITE;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        createNotificationChannel();
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(101, notification);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createBubble();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "PuniMenu", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("フローティングメニューの実行通知");
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return b.setContentTitle("PuniMenu 起動中")
                .setContentText("通知からアプリを終了できます")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(12, 9, 12, 9);
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(bg);
        return l;
    }

    private void createBubble() {
        TextView b = text("P", 20, white);
        b.setGravity(Gravity.CENTER);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackgroundColor(Color.rgb(113, 52, 180));
        bubble = b;
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        bubbleParams = new WindowManager.LayoutParams(58, 58, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 24;
        bubbleParams.y = 220;
        windowManager.addView(bubble, bubbleParams);

        b.setOnClickListener(v -> togglePanel());
        b.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float downX, downY;
            private boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = bubbleParams.x; startY = bubbleParams.y;
                    downX = e.getRawX(); downY = e.getRawY(); moved = false;
                    return false;
                } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    int dx = (int)(e.getRawX() - downX), dy = (int)(e.getRawY() - downY);
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) moved = true;
                    if (moved) {
                        bubbleParams.x = startX + dx; bubbleParams.y = startY + dy;
                        windowManager.updateViewLayout(bubble, bubbleParams);
                    }
                    return moved;
                }
                return false;
            }
        });
    }

    private void togglePanel() {
        if (panel == null) showPanel();
        else {
            try { windowManager.removeView(panel); } catch (Exception ignored) {}
            panel = null;
        }
    }

    private void showPanel() {
        LinearLayout root = vertical();
        root.setPadding(10, 10, 10, 10);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("PuniMenu  /  ARM64", 18, purple);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button close = new Button(this);
        close.setText("×");
        header.addView(close, new LinearLayout.LayoutParams(52, 48));
        root.addView(header);

        TextView disclaimer = text("設定UIサンプル（ゲームへのパッチ適用なし）", 11, Color.LTGRAY);
        root.addView(disclaimer);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = vertical();
        content.setPadding(0, 4, 0, 4);
        addSwitch(content, "ワンパン", "one_hit", false);
        addSwitch(content, "倍速機能", "speed", false);
        addSwitch(content, "遅延設定", "delay", false);
        addSwitch(content, "無敵", "invincible", false);
        addSwitch(content, "スコア設定", "score", false);
        addSwitch(content, "敵ターン無効", "enemy_turn", false);
        addSwitch(content, "即フィーバー", "instant_fever", false);
        addSwitch(content, "無限フィーバー", "infinite_fever", false);
        addSwitch(content, "フィーバーゲージ増加", "fever_gauge", false);
        addSwitch(content, "リザルトスキップ", "result_skip", false);
        addSwitch(content, "開始時必殺技", "special_start", false);
        addSwitch(content, "最高好物表示", "favorite_food", false);
        addSwitch(content, "スキル2設定", "skill2", false);

        addSeekBar(content, "速度（表示設定）", "speed_value", 10, 20, 15, "1.0×", "2.0×");
        addSeekBar(content, "ダメージ倍率（表示設定）", "damage_value", 1, 10, 1, "1×", "98304×");

        TextView note = text("注意：スイッチはこのアプリ内に保存される設定です。ゲームの動作は変更しません。", 12, Color.LTGRAY);
        root.addView(note);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 620));

        close.setOnClickListener(v -> togglePanel());

        panel = root;
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        panelParams = new WindowManager.LayoutParams(330, -2, type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = 90;
        panelParams.y = 160;
        windowManager.addView(panel, panelParams);
    }

    private void addSwitch(LinearLayout parent, String label, String key, boolean defaultValue) {
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setTextColor(white);
        sw.setPadding(12, 8, 12, 8);
        sw.setShowText(false);
        sw.setChecked(prefs.getBoolean(key, defaultValue));
        sw.setOnCheckedChangeListener((buttonView, checked) -> prefs.edit().putBoolean(key, checked).apply());
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(card);
        row.addView(sw, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = 5;
        parent.addView(row, lp);
    }

    private void addSeekBar(LinearLayout parent, String label, String key, int min, int max, int def, String low, String high) {
        LinearLayout box = vertical();
        box.setBackgroundColor(card);
        TextView title = text(label, 14, white);
        box.addView(title);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(Math.max(0, Math.min(max - min, prefs.getInt(key, def) - min)));
        box.addView(seek);
        TextView value = text(low + "  —  " + high, 12, purple);
        box.addView(value);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int val = progress + min;
                prefs.edit().putInt(key, val).apply();
                value.setText(label + ": " + val);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = 7;
        parent.addView(box, lp);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (windowManager != null) {
            if (bubble != null) try { windowManager.removeView(bubble); } catch (Exception ignored) {}
            if (panel != null) try { windowManager.removeView(panel); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
