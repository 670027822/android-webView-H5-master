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
import android.graphics.Color

class ForegroundService : Service() {
    private val CHANNEL_ID = "ForegroundServiceChannel"
    private val BASE_NOTIFICATION_ID = 1000  // 修改基础通知ID
    private var notificationCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            addNewNotification()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            // 确保首次启动时也计数为1
            notificationCount++
            val notification = createNotification(BASE_NOTIFICATION_ID, "服务已启动")
            startForeground(BASE_NOTIFICATION_ID, notification)
            startServiceAutoRestart()
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
                NotificationManager.IMPORTANCE_DEFAULT  // 降低重要性级别
            ).apply {
                description = "保持应用在后台运行"
                setShowBadge(true)
                enableVibration(false)  // 关闭振动
                enableLights(true)
                lightColor = Color.BLUE
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(id: Int, message: String): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 为每个通知创建唯一的通知组
        val groupKey = "notification_group_${System.currentTimeMillis()}"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("运行中")
            .setContentText("已产生 $notificationCount 条通知 - $message")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setNumber(notificationCount)
            .setOngoing(true)
            .setAutoCancel(false)
            .setGroup(groupKey)
            .setGroupSummary(id == BASE_NOTIFICATION_ID)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun addNewNotification() {
        notificationCount++
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 更新主通知
        val mainNotification = createNotification(BASE_NOTIFICATION_ID, "服务运行中")
        notificationManager.notify(BASE_NOTIFICATION_ID, mainNotification)

        // 创建新的子通知
        val newNotificationId = BASE_NOTIFICATION_ID + notificationCount
        val newNotification = createNotification(
            newNotificationId,
            "这是第 $notificationCount 条通知"
        )
        notificationManager.notify(newNotificationId, newNotification)

        // 如果通知数量过多，删除旧的通知
        if (notificationCount > 5) {
            notificationManager.cancel(BASE_NOTIFICATION_ID + (notificationCount - 5))
        }
    }

    private fun startServiceAutoRestart() {
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