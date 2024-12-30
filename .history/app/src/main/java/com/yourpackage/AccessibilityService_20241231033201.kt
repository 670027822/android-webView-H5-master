import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.os.Handler
import android.os.Looper
import android.util.Log

class MyAccessibilityService : AccessibilityService() {
    
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    
    private val keepAliveRunnable = object : Runnable {
        override fun run() {
            try {
                // 执行轻量级任务，仅记录日志
                Log.d("AccessibilityService", "保持服务活跃: ${System.currentTimeMillis()}")
                
                // 每30秒执行一次
                handler.postDelayed(this, 30_000)
            } catch (e: Exception) {
                Log.e("AccessibilityService", "保活任务执行失败", e)
            }
        }
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        serviceInfo = info
        
        // 开始执行保活任务
        startKeepAliveTask()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 仅监听窗口变化事件
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                try {
                    if (event.packageName != null && 
                        event.packageName.toString() != packageName) {
                        // 应用切换时记录日志
                        Log.d("AccessibilityService", 
                            "检测到窗口切换: ${event.packageName}")
                    }
                } catch (e: Exception) {
                    Log.e("AccessibilityService", "处理窗口事件失败", e)
                }
            }
        }
    }

    private fun startKeepAliveTask() {
        if (!isRunning) {
            isRunning = true
            handler.post(keepAliveRunnable)
        }
    }

    private fun stopKeepAliveTask() {
        isRunning = false
        handler.removeCallbacks(keepAliveRunnable)
    }

    override fun onInterrupt() {
        stopKeepAliveTask()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopKeepAliveTask()
    }
} 