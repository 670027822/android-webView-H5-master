import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ForegroundService : Service() {
    private val CHANNEL_ID = "ForegroundServiceChannel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "前台服务通知",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "保持应用在后台运行"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("应用正在运行")
            .setContentText("点击管理应用")
            .setSmallIcon(R.drawable.ic_notification) // 请确保添加通知图标
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
} 