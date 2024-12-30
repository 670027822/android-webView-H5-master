import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.content.Intent

class MyAccessibilityService : AccessibilityService() {
    
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
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // 检测应用是否进入后台
                if (event.packageName != null && event.packageName != packageName) {
                    // 启动前台服务
                    startForegroundService(Intent(this, ForegroundService::class.java))
                }
            }
        }
    }

    override fun onInterrupt() {
        // 服务中断时的处理
    }
} 