import android.app.*
import android.content.Intent
import android.content.PendingIntent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.content.Context
import android.app.AlarmManager
import android.os.Handler
import android.os.Looper

class ForegroundService : Service() {
    private val CHANNEL_ID = "ForegroundServiceChannel"
    private val NOTIFICATION_ID = 1
    private var notificationCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            addNewNotification()
            handler.postDelayed(this, 5000) // 每5秒执行一次
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        try {
            startForeground(NOTIFICATION_ID, notification)
            startServiceAutoRestart()
            // 开始定期添加通知
            handler.post(updateRunnable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "前台服务通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "保持应用在后台运行"
                setShowBadge(true)  // 允许显示角标
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(id: Int = NOTIFICATION_ID, message: String = "应用正在运行"): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("运行中 (${notificationCount}条通知)")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(notificationCount)  // 设置通知数量角标
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun addNewNotification() {
        notificationCount++
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 更新主通知
        notificationManager.notify(NOTIFICATION_ID, createNotification())
        
        // 创建新的通知
        val newNotificationId = NOTIFICATION_ID + notificationCount
        val newNotification = createNotification(
            id = newNotificationId,
            message = "第 $notificationCount 条通知"
        )
        notificationManager.notify(newNotificationId, newNotification)
    }

    private fun startServiceAutoRestart() {
        // 现有的重启逻辑保持不变
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ForegroundService::class.java)
        val pendingIntent = PendingIntent.getService(
            this,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 15 * 60 * 1000,
            15 * 60 * 1000,
            pendingIntent
        )
    }
} 