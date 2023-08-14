package com.akash.location

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters


class LocationServiceDestroyWorker(context: Context, params: WorkerParameters) :
    Worker(context, params) {
    val context = context
    override fun doWork(): Result {

        destroySerivce()
        return Result.success()
    }

    companion object {
         fun destroySerivce() {
            val locationServieStopIntent = Intent(LocationDriver.clientApplicationInstance!!.applicationContext, LocationService::class.java)
            locationServieStopIntent.action =
                Constants.DESTROY_LOCATION_SERVICE_ACTION_WITH_ACTIVITY_RECOGNITION
             LocationDriver.clientApplicationInstance!!.applicationContext.startService(locationServieStopIntent)
        }
    }
}
