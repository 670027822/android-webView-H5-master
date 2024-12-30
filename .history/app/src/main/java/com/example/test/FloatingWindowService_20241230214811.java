package com.example.test;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

public class FloatingWindowService extends Service {
    private static final String TAG = "FloatingWindowService";
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private WebView webView;
    private PowerManager.WakeLock wakeLock;
    
    private BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                if (floatingView != null && floatingView.getParent() != null) {
                    windowManager.removeView(floatingView);
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                if (floatingView != null && floatingView.getParent() == null) {
                    try {
                        windowManager.addView(floatingView, params);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: 服务创建");
        createFloatingWindow();
        setupWebView();
        
        // 注册屏幕监听
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, filter);
        
        // 获取电源锁
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp:WebViewWakeLock");
        wakeLock.acquire();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        Log.d(TAG, "setupWebView: 设置WebView");
        webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());
        
        // 配置 WebView 设置
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        // 加载网页
        webView.loadUrl("https://tv.aizhijia.top/naozhong.html");
        
        // 添加到悬浮窗
        ((LinearLayout) floatingView).addView(webView, 1, 1);
        
        // 添加 JavaScript 接口来保持活跃
        webView.evaluateJavascript(
            "setInterval(function() { " +
            "   console.log('keepAlive');" +
            "}, 1000);", null
        );
    }

    private void createFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = new LinearLayout(this);
        floatingView.setBackgroundColor(0x00000000);

        params = new WindowManager.LayoutParams(
                1, // 设置最小尺寸
                1,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, // 保持屏幕常亮
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.START | Gravity.TOP;
        
        try {
            windowManager.addView(floatingView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: 服务销毁");
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
        if (windowManager != null && floatingView != null) {
            windowManager.removeView(floatingView);
        }
        unregisterReceiver(screenReceiver);
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: 服务启动");
        // 获取传递过来的URL
        String url = intent.getStringExtra("current_url");
        if (url != null && webView != null) {
            Log.d(TAG, "加载URL: " + url);
            webView.loadUrl(url);
        }
        return START_STICKY;
    }
} 