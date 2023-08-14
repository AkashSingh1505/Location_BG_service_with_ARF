package com.akash.location

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView


class LocationOverlayDialog() :
    Application.ActivityLifecycleCallbacks {

    companion object {
        private var dialog: LocationOverlayDialog? = null
        private var context: Context? = null
        private var windowManager: WindowManager? = null
        private var activityCount = 0
        private var overlayView: View? = null
        fun getInstance(context: Context): LocationOverlayDialog {
            dialog?.let { return it } ?: kotlin.run {
                this.context = context
                dialog = LocationOverlayDialog()
                return dialog as LocationOverlayDialog
            }
        }
        fun show() {
            val params = createLayoutParams()

            // Inflate your custom dialog view
            overlayView =
                if(LocationDriver.configSetup?.getInfoDialogLayout()==-1) {
                    if (LocationDriver.configSetup?.getIsARFOn()!! == true) {
                        LayoutInflater.from(context).inflate(R.layout.info_dialog_with_arf, null)
                    } else {
                        LayoutInflater.from(context).inflate(R.layout.info_dialog, null)
                    }
                }else{
                    LayoutInflater.from(context).inflate(LocationDriver.configSetup!!.getInfoDialogLayout(), null)
                }

            // Add the view to the window manager
            windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager?.addView(overlayView, params)
            Handler().postDelayed({ hide() },LocationDriver.configSetup?.getInfoDialogDuration()!!)
            overlayView?.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    hide()
                    true
                } else {
                    false
                }
            }
        }

        fun invisible(){
            overlayView?.visibility=View.INVISIBLE
        }
        fun visible(){
            overlayView?.visibility=View.VISIBLE
        }

        fun hide() {
            if (overlayView != null && windowManager != null) {
                windowManager?.removeView(overlayView)
                overlayView = null
                windowManager = null
            }
        }

        private fun createLayoutParams(): WindowManager.LayoutParams {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.CENTER
            return params
        }


    }



    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        println("")
    }

    override fun onActivityStarted(p0: Activity) {
        if (activityCount == 0) {
            //an app entered in forground
            visible()
        }
        activityCount++
    }

    override fun onActivityResumed(p0: Activity) {
        println("")
    }

    override fun onActivityPaused(p0: Activity) {
        println("")
    }

    override fun onActivityStopped(p0: Activity) {
        activityCount--
        if (activityCount == 0) {
            // App entered background
            invisible()
        }
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        println("")
    }

    override fun onActivityDestroyed(p0: Activity) {
        println("")
    }
}
