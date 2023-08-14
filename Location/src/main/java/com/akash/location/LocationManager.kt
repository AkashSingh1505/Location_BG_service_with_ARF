package com.akash.location

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class LocationManager() {
    companion object {
        val context = LocationDriver.clientApplicationInstance!!!!.applicationContext
        fun runDriver(activityContext: Context) {
            if(!LocationUtill.isServiceIsRunning(context,LocationService::class.java) &&
                System.currentTimeMillis()<LocationUtill.timeInMillis(
                    LocationDriver.configSetup?.getEHour()!!,
                    LocationDriver.configSetup?.getEMinutes()!!,
                    LocationDriver.configSetup?.getESecond()!!
                )){
                runServiceStarter(
                    LocationDriver.configSetup?.getSHour()!!,
                    LocationDriver.configSetup?.getSMinutes()!!,
                    LocationDriver.configSetup?.getSSecond()!!
                )
            }else {
                Log.i(
                    TAG,
                    "Service is already running or the current time not falls within the working hour."
                )
            }
        }

        private fun runServiceStarter(h:Int,m:Int,s:Int) {
            val workRequest = OneTimeWorkRequestBuilder<LocationServiceStarterWorker>()
                .setInitialDelay(
                    LocationUtill.getDelaysInTargetTime(h,m,s),
                    TimeUnit.MILLISECONDS
                )
                .build()
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                Constants.SERVICE_CREATER_WORKER_NAME,
                ExistingWorkPolicy.REPLACE, workRequest
            )
            val pid = android.os.Process.myPid()
            Log.d(TAG, "worker PID: $pid")
            Log.i(TAG,"Service Starter is running in order to start the service on the specific time...")
        }
    }
}