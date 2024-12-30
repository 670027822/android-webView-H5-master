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
import androidx.core.app.NotificationCompat;

public class FloatingWindowService extends Service {
    private static final String TAG = "FloatingWindowService";
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private WebView webView;
    private PowerManager.WakeLock wakeLock;
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "floating_window_channel";
    
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
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        try {
            webView = new WebView(getApplicationContext());
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    Log.d(TAG, "网页加载完成: " + url);
                }
            });
            
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setMediaPlaybackRequiresUserGesture(false);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
            
            // 设置 WebView 大小
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    300,  // 设置固定宽度
                    200   // 设置固定高度
            );
            webView.setLayoutParams(layoutParams);
            
            // 添加到悬浮窗
            ((LinearLayout) floatingView).addView(webView);
            
            // 加载网页
            webView.loadUrl("https://tv.aizhijia.top/naozhong.html");
            
        } catch (Exception e) {
            Log.e(TAG, "setupWebView: WebView设置失败", e);
        }
    }

    private void createFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = new LinearLayout(this);
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,  // 改为自适应大小
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.START | Gravity.TOP;
        
        try {
            windowManager.addView(floatingView, params);
            Log.d(TAG, "悬浮窗创建成功");
        } catch (Exception e) {
            Log.e(TAG, "createFloatingWindow: 悬浮窗创建失败", e);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: 服务销毁");
        try {
            if (webView != null) {
                webView.stopLoading();
                webView.clearHistory();
                ((LinearLayout) floatingView).removeView(webView);
                webView.destroy();
                webView = null;
            }
            if (windowManager != null && floatingView != null) {
                windowManager.removeView(floatingView);
            }
            unregisterReceiver(screenReceiver);
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "onDestroy: 清理资源失败", e);
        }
        super.onDestroy();
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("应用正在后台运行")
            .setContentText("保持应用活跃中...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)  // 使用系统图标
            .setPriority(NotificationCompat.PRIORITY_LOW);
            
        return builder.build();
    }
} 