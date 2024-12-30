package com.example.test;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.app.NotificationCompat;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
import android.annotation.SuppressLint;
import android.content.Context;

public class ForegroundService extends Service {

    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private WebView webView;
    private PowerManager.WakeLock wakeLock;
    private Handler handler = new Handler();
    private Timer keepAliveTimer;
    private NotificationManager notificationManager;
    private int notificationCounter = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
        acquireWakeLock();
        setupWebView();
        startKeepAliveTimer();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView = new WebView(getApplicationContext());
        webView.setWebViewClient(new WebViewClient());
        
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.loadUrl("https://tv.aizhijia.top/naozhong.html");
    }

    private void startKeepAliveTimer() {
        keepAliveTimer = new Timer();
        keepAliveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    // 更新 WebView
                    if (webView != null) {
                        webView.loadUrl("javascript:(function() { " +
                            "var audio = document.getElementById('alarmSound');" +
                            "if(audio) { audio.load(); }" +
                            "console.log('Service keepAlive: ' + new Date());" +
                            "})()");
                    }
                    
                    // 更新通知
                    updateNotification();
                });
            }
        }, 0, 10000); // 每10秒执行一次
    }

    private void updateNotification() {
        notificationCounter++;
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("应用正在运行中")
                .setContentText("保持运行中... " + notificationCounter)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(createPendingIntent())
                .build();

        notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private PendingIntent createPendingIntent() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "前台服务通知",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("保持应用在后台运行");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(true);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setBypassDnd(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("应用正在运行中")
                .setContentText("保持运行中...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(createPendingIntent())
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 确保服务在前台运行
        startForeground(NOTIFICATION_ID, createNotification());
        
        // 如果服务被系统杀死后重启
        if (intent == null) {
            setupWebView();
            startKeepAliveTimer();
        }
        
        return START_STICKY;
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK | 
            PowerManager.ON_AFTER_RELEASE,
            "MyApp:ForegroundServiceLock");
        wakeLock.acquire();
    }

    @Override
    public void onDestroy() {
        try {
            // 在服务被销毁前尝试重启
            Intent restartService = new Intent(getApplicationContext(), ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartService);
            } else {
                startService(restartService);
            }

            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
            if (keepAliveTimer != null) {
                keepAliveTimer.cancel();
            }
            if (webView != null) {
                webView.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 