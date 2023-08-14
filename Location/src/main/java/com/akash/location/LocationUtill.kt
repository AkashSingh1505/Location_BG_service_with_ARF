package com.akash.location

import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import java.util.Calendar

object  LocationUtill {
     fun getDelaysInTargetTime(h:Int,m:Int,s:Int):Long{
        val targetTime = timeInMillis(h,m,s)
        val currentTime= System.currentTimeMillis()
        return if(targetTime>=currentTime)
            targetTime-currentTime
        else
            0
    }
    fun timeInMillis(hour: Int, min: Int, sec: Int):Long{
        val calendar = Calendar.getInstance()
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, min)
            set(Calendar.SECOND, sec)
        }
        return calendar.timeInMillis
    }
    fun isServiceIsRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = manager.getRunningServices(Int.MAX_VALUE)

        for (service in runningServices) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
    fun showLocationTrackAlertDialog(context: Context) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.info_dialog_with_arf)
        dialog.show()
    }
}