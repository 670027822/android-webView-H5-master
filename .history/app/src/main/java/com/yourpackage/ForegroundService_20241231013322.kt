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
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioManager

class ForegroundService : Service() {
    private val CHANNEL_ID = "ForegroundServiceChannel"
    private val NOTIFICATION_ID = 1
    private var notificationCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            addNewNotification()
            handler.postDelayed(this, 5000) // 每5秒执行一次
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initSilentMusic()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        try {
            startForeground(NOTIFICATION_ID, notification)
            startServiceAutoRestart()
            handler.post(updateRunnable)
            startSilentMusic()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        stopSilentMusic()
    }

    private fun initSilentMusic() {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource("android.resource://${packageName}/raw/silence")
                setVolume(0f, 0f) // 设置音量为0
                isLooping = true // 循环播放
                prepare()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startSilentMusic() {
        try {
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopSilentMusic() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
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