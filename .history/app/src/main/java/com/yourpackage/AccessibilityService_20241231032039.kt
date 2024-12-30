import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.util.Log

class MyAccessibilityService : AccessibilityService() {
    
    private var isServiceRunning = false
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or 
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        serviceInfo = info
        
        // 服务连接时启动前台服务
        try {
            if (!isServiceRunning) {
                isServiceRunning = true
                val serviceIntent = Intent(this, ForegroundService::class.java)
                startForegroundService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("AccessibilityService", "启动前台服务失败", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    // 检测应用是否进入后台
                    if (event.packageName != null && 
                        event.packageName.toString() != packageName && 
                        !isServiceRunning) {
                        isServiceRunning = true
                        val serviceIntent = Intent(this, ForegroundService::class.java)
                        startForegroundService(serviceIntent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AccessibilityService", "处理无障碍事件失败", e)
        }
    }

    override fun onInterrupt() {
        isServiceRunning = false
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
    }
} 