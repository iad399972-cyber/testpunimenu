package com.example.punimenu;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;

public class MainActivity extends Activity {
    private static final int REQUEST_OVERLAY = 900;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 36, 28, 28);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(18, 14, 28));

        TextView title = new TextView(this);
        title.setText("PuniMenu");
        title.setTextColor(Color.rgb(205, 160, 255));
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView info = new TextView(this);
        info.setText("黒×紫のフローティング設定パネル\n\n「他のアプリの上に重ねて表示」を許可してから起動してください。\nこのアプリはメニューUIのサンプルです。ゲームのメモリ変更・パッチ適用機能は含みません。");
        info.setTextColor(Color.WHITE);
        info.setTextSize(15);
        info.setPadding(0, 24, 0, 24);
        root.addView(info, new LinearLayout.LayoutParams(-1, -2));

        Button permission = new Button(this);
        permission.setText("オーバーレイ権限を設定");
        root.addView(permission, new LinearLayout.LayoutParams(-1, -2));
        permission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY);
            } else {
                startOverlay();
            }
        });

        Button start = new Button(this);
        start.setText("メニューを起動");
        root.addView(start, new LinearLayout.LayoutParams(-1, -2));
        start.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } else {
                startOverlay();
            }
        });

        Button stop = new Button(this);
        stop.setText("メニューを終了");
        root.addView(stop, new LinearLayout.LayoutParams(-1, -2));
        stop.setOnClickListener(v -> stopService(new Intent(this, OverlayService.class)));

        setContentView(root);
    }

    private void startOverlay() {
        Intent service = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service);
        } else {
            startService(service);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))) {
            startOverlay();
        }
    }
}
