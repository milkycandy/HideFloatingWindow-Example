package cn.milkycandy.hidefloatingwindow

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager

class FloatingWindowService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingWindow()
        // 延迟一下再调用，确保view已经创建
        Handler(Looper.getMainLooper()).postDelayed({
            applySkipScreenshot()
        }, 100)
    }

    private fun createFloatingWindow() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null, false)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        windowManager.addView(floatingView, params)
    }

    private fun applySkipScreenshot() {
        try {
            val viewRoot = View::class.java.getDeclaredMethod("getViewRootImpl")
                .apply { isAccessible = true }
                .invoke(floatingView) ?: return

            val surfaceControl = viewRoot.javaClass.getDeclaredField("mSurfaceControl")
                .apply { isAccessible = true }
                .get(viewRoot)

            val transactionClass = Class.forName("android.view.SurfaceControl\$Transaction")
            val transaction = transactionClass.getDeclaredConstructor().newInstance()

            transactionClass.getMethod(
                "setSkipScreenshot",
                Class.forName("android.view.SurfaceControl"),
                Boolean::class.java
            ).invoke(transaction, surfaceControl, true)

            transactionClass.getMethod("apply").invoke(transaction)

            Log.d(TAG, "成功应用截图跳过")
        } catch (e: Exception) {
            Log.e(TAG, "应用截图跳过时失败", e)
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }

    companion object {
        private const val TAG = "FloatingWindowService"
    }
}