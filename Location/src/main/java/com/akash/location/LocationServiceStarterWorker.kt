package com.akash.location

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log
import androidx.work.WorkManager


class LocationServiceStarterWorker(context: Context, parms: WorkerParameters) :
    Worker(context, parms) {
    val context = context
    override fun doWork(): Result {
        try {
            startService()
            WorkManager.getInstance(applicationContext).cancelUniqueWork(Constants.SERVICE_CREATER_WORKER_NAME)
            Log.i(TAG,"Service starter has cancled after Completion of work")
            return Result.success()
        }catch (e :Exception){
            Log.e(TAG,"Service Starter got failed due to error : ${e}")
            return Result.failure()
        }


    }

    private fun startService(){
        val appBootIntent = Intent(applicationContext, LocationService::class.java)
        appBootIntent?.putExtra(
            Constants.ACTIVITY_TYPE_KEY, 4
        )// for unknown predefined value = 4
        appBootIntent?.putExtra(Constants.TRANSACTION_TYPE_KEY, 4)
        appBootIntent.action = Constants.APPLICATION_FIRST_BOOT_ACTION
        applicationContext.startService(appBootIntent)
        Log.i(
            TAG,
            "The current time falls within the working hance Location Service has Started Now"
        )
    }
}