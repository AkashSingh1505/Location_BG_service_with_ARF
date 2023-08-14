package com.akash.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransitionResult
import java.text.SimpleDateFormat
import java.util.*


class ActivityTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appBootIntent = Intent(context, LocationService::class.java)
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.let {
                result.transitionEvents.forEach { event ->
                    //Info for debugging purposes
                    val info =
                        "Transition: " + ActivityTransitionsUtil.toActivityString(event.activityType) + " (" + ActivityTransitionsUtil.toTransitionType(
                            event.transitionType
                        ) + ")" + "   " + SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                    Log.i(TAG, info)
                    appBootIntent.action = Constants.ACTIVITY_DETECTED_ACTION
                    appBootIntent?.putExtra(Constants.ACTIVITY_TYPE_KEY, event.activityType)
                    appBootIntent?.putExtra(
                        Constants.TRANSACTION_TYPE_KEY, event.transitionType
                    )
                    context?.startService(appBootIntent)
                }
            }
        }
    }
}