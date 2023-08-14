package com.akash.location

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.TimeUnit



class LocationService : Service() {
    companion object {
        val cordinates: MutableLiveData<Pair<Double, Double>> = MutableLiveData()
        val activityAndTransition: MutableLiveData<Pair<String, String>> = MutableLiveData()
    }
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var activityRecognitionClient: ActivityRecognitionClient
    private  var activityPendingIntent: PendingIntent?=null
    private var location_interval : Long = LocationDriver.configSetup?.getServiceStaticIntervalInMillis ()!!
    private var activityType: Int = 0
    private var transactionType : Int =0
    private var savedLocationServiceIntent : Intent?=null
    private var savedActivityRecognitionPendingIntent : PendingIntent?=null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        if(LocationDriver.configSetup?.getIsARFOn()==true){
            activityRecognitionClient = ActivityRecognition.getClient(this)
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val lastLocation = locationResult.lastLocation
                try {
                    Log.i(TAG,"API hit at : ${timeNow()}")
                    cordinates.value = Pair(lastLocation?.longitude ?: 0.0, lastLocation?.latitude ?: 0.0)
                    showLocation(lastLocation)
                }catch (e : java.lang.Exception){
                    Log.e(TAG,"Unable to find host_name")
                }

            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopActivityRecognitionUpdates(pendingIntent: PendingIntent) {
        activityRecognitionClient.removeActivityTransitionUpdates(pendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Activity recognition updates stopped.")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to stop activity recognition updates: ${e.message}")
            }
    }

    private fun showLocation(location: Location?) {
        Log.i(TAG, "Latitude-> ${location?.latitude}, Longitude-> ${location?.longitude}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val pid = android.os.Process.myPid()
        Log.d(TAG, "Service PID: $pid")
        if (intent != null) {
            // Save the intent for future retrieval
            saveIntent(intent)
            processIntent(intent)
        }
        else{
            Log.i(TAG,"Application killed by the OS, Location Service restarting process...")
            // Retrieve the saved intent if available
            try{
                savedLocationServiceIntent = retriveIntent()
                if (savedLocationServiceIntent != null) {
                    // Process the retrieved intent
                    processIntent(savedLocationServiceIntent!!)
                }else{
                    Log.i(TAG,"Unable to Start Service Due to Intent NULL Found")
                }
            }catch (e : IOException){
                Log.e(TAG,"Unable to start the service Due to error :${e}")
            }
        }
        return START_STICKY
    }
    private fun savePendingIntent(parentIntent : Intent){
        val sharedPreferences = applicationContext.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val serializedIntent = SerializedIntent(parentIntent.action,parentIntent.component)
        val intentJson = intentToJson(serializedIntent) // Convert the intent to JSON string
        editor.putString("activity_reognition_intent_key", intentJson)
        editor.putInt("request_code_pending_intent_key",Constants.REQUEST_CODE_PENDING_INTENT_ACTIVITY_TRANSITION)
        editor.apply()

        Log.i(TAG,"Activity reognition pendingIntent has saved")
    }

    @Throws(IOException::class)
    private fun retrivePendingIntent():PendingIntent{
        try {
            val sharedPreferences =
                applicationContext.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
            val intentJson = sharedPreferences.getString("activity_reognition_intent_key", null)
            if (intentJson != null) {
                val intent = intentFromJson(intentJson) // Convert the JSON string back to an intent
                val requestCode = sharedPreferences.getInt("request_code_pending_intent_key",0)
                Log.i(TAG,"Activity reognition pendingIntent retrive successfully")
                return PendingIntent.getBroadcast(
                    applicationContext,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            }
        }catch (e : IOException){
            throw e
            Log.i(TAG,"Unable to de_register_activity_recognition due to lost pending intent with this error : ${e.toString()}")
        }
        Log.i(TAG,"Unable to de_register_activity_recognition due to lost pending intent with this error : ${IOException()}")
        return throw IOException()

    }
    private fun processIntent(intent: Intent) {
        setLocationIntervalValues(intent)
        when(intent?.action){
            Constants.APPLICATION_FIRST_BOOT_ACTION->{
                Log.i(TAG, "Service has started at ${timeNow()}")
                if(LocationDriver.configSetup?.getIsARFOn()==true){
                    startActivityRecognitionUpdates()
                }
                startForeground(Constants.NOTIFICATION_ID, getNotification())
                LocationOverlayDialog.show()
                createLocationRequest()
                serviceDistroyeWorker(
                    LocationDriver.configSetup?.getEHour()!!,
                    LocationDriver.configSetup?.getEMinutes()!!,
                    LocationDriver.configSetup?.getESecond()!!
                )
            }

            Constants.DESTROY_LOCATION_SERVICE_ACTION_WITH_ACTIVITY_RECOGNITION->{
                activityPendingIntent?.let {
                    destroyService(activityPendingIntent!!)
                }?:kotlin.run {
                    try {
                        savedActivityRecognitionPendingIntent = retrivePendingIntent()
                        savedActivityRecognitionPendingIntent?.let {
                            destroyService(savedActivityRecognitionPendingIntent!!)
                        }
                    }catch (e : IOException){
                        throw e
                    }

                }
            }

            else->{
                Log.i(TAG, "Service has re-started at ${timeNow()}")
                startForeground(Constants.NOTIFICATION_ID, getNotification())
                createLocationRequest()
            }
        }
    }

    private fun serviceDistroyeWorker(h:Int,m:Int,s:Int) {

        val workeRequest = OneTimeWorkRequestBuilder<LocationServiceDestroyWorker>()
            .setInitialDelay(LocationUtill.getDelaysInTargetTime(h,m,s),TimeUnit.MILLISECONDS)
            .build()

        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueueUniqueWork(Constants.SERVICE_DESTROYER_WORKER_NAME,ExistingWorkPolicy.REPLACE,workeRequest)
        if(h>12){
            Log.i(TAG,"Service will end at : ${h-12}:${m}:${s} PM")
        }else{
            Log.i(TAG,"Service will end at : ${h}:${m}:${s} AM")
        }
    }


    private fun saveIntent(intent: Intent) {
        if(savedLocationServiceIntent!=null){
            clearIntent()
        }
        val sharedPreferences = applicationContext.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val serializedIntent = SerializedIntent(intent.action, intent.component)
        val intentJson = intentToJson(serializedIntent)// Convert the intent to JSON string
        editor.putString("SavedIntent", intentJson)
        transactionType = intent?.getIntExtra(Constants.TRANSACTION_TYPE_KEY,4)!!
        activityType = intent?.getIntExtra(Constants.ACTIVITY_TYPE_KEY,4)!!
        editor.putInt(Constants.TRANSACTION_TYPE_KEY,transactionType)
        editor.putInt(Constants.ACTIVITY_TYPE_KEY,activityType)
        editor.apply()
        Log.i(TAG,"Location Service Intent Saved")
    }
    private fun intentToJson(intent: SerializedIntent): String {
        val gson = Gson()
        return gson.toJson(intent)
    }

    private fun intentFromJson(json: String): Intent {
        val gson = Gson()
        return gson.fromJson(json, Intent::class.java)
    }

    private fun retriveIntent(): Intent? {
        try{
            val sharedPreferences =
                applicationContext.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
            val intentJson = sharedPreferences.getString("SavedIntent", null)
            var activityType = sharedPreferences.getInt(Constants.ACTIVITY_TYPE_KEY,4)
            var transactionType = sharedPreferences.getInt(Constants.TRANSACTION_TYPE_KEY,4)
            return if (intentJson != null) {
                val intent = intentFromJson(intentJson) // Convert the JSON string back to an intent
                intent.putExtra(Constants.ACTIVITY_TYPE_KEY,activityType)
                intent.putExtra(Constants.TRANSACTION_TYPE_KEY,transactionType)
                Log.i(TAG,"Locatio service intent retrive successfully ")
                intent
            } else {
                Log.i(TAG,"Locatio service intent retrive Faild ")
                null
            }
        }catch (e : IOException){
            Log.e(TAG,e.toString())
        }
        return null
    }

    private fun clearIntent() {
        val sharedPreferences = applicationContext.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("SavedIntent")
        editor.apply()
        Log.i(TAG,"Previous Location service intent cleared as no longer needed.")
    }

    @SuppressLint("MissingPermission")
    private fun startActivityRecognitionUpdates() {
        activityRecognitionClient = ActivityRecognition.getClient(applicationContext)
        activityPendingIntent =getPendingIntent()

        activityRecognitionClient.requestActivityTransitionUpdates(
            ActivityTransitionsUtil.getActivityTransitionRequest(),
            activityPendingIntent!!
        )
            .addOnSuccessListener {
                Log.i(TAG, "Activity recognition updates started.")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to start activity recognition updates: ${e.message}")
            }
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(LocationManager.context, ActivityTransitionReceiver::class.java)
        savePendingIntent(intent)

        return PendingIntent.getBroadcast(
            applicationContext,
            Constants.REQUEST_CODE_PENDING_INTENT_ACTIVITY_TRANSITION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun setLocationIntervalValues(intent: Intent?) {
        transactionType = intent?.getIntExtra(Constants.TRANSACTION_TYPE_KEY,4)!!
        activityType = intent?.getIntExtra(Constants.ACTIVITY_TYPE_KEY,4)!!
        activityAndTransition.value = Pair(ActivityTransitionsUtil.toActivityString(activityType),transactionType.toString())
        when(activityType){
            DetectedActivity.IN_VEHICLE->{location_interval=LocationDriver.configSetup!!.getVehicle()}
            DetectedActivity.RUNNING->{location_interval=LocationDriver.configSetup!!.getRun()}
            DetectedActivity.STILL->{location_interval=LocationDriver.configSetup!!.getStill()}
            DetectedActivity.WALKING->{location_interval=LocationDriver.configSetup!!.getWalk()}
            DetectedActivity.UNKNOWN->{location_interval=LocationDriver.configSetup!!.getUnknown ()}
        }

    }

    @SuppressLint("MissingPermission")
    private fun createLocationRequest() {
        try {
            locationRequest =
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, location_interval).build()
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.myLooper())
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun destroyService(pendingIntent: PendingIntent){
        Log.i(TAG, "Service  destroing process start... at ${timeNow()}")
        clearIntent()
        if(LocationDriver.configSetup?.getIsARFOn()==true){
            clearPendingIntent()
            stopActivityRecognitionUpdates(pendingIntent)
        }
        locationCallback?.let { fusedLocationProviderClient?.removeLocationUpdates(it) }
        stopForeground(true)
        stopService(Intent(applicationContext,LocationService::class.java))
        stopSelf()
        WorkManager.getInstance(applicationContext).cancelUniqueWork(Constants.SERVICE_DESTROYER_WORKER_NAME)
        Log.i(TAG, "Service  destroing process Ended at ${timeNow()}")
    }

    private fun clearPendingIntent() {
        val sharedPreferences = applicationContext.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("pendingIntent")
        editor.apply()
        Log.i(TAG,"activityRecognitionPending intent cleared as no longer needed.")
    }

    private fun getBitmap(rid: Int): Bitmap {
        try {
            val drawable = ResourcesCompat.getDrawable(resources, rid, null)
            val bitmapDrawable = drawable as BitmapDrawable
            if (bitmapDrawable is BitmapDrawable) {
                return bitmapDrawable.bitmap
            } else {
                // Handle the case where the drawable is not a BitmapDrawable
                // This can happen if the resource ID does not correspond to a valid drawable
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Return an empty 1x1 bitmap
            }
        } catch (e: Exception) {
            // Handle any exceptions that might occur during resource retrieval
            e.printStackTrace()
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Return an empty 1x1 bitmap
        }
    }

    private fun getNotification(): Notification {
        var title: String = ""
        var content: String = ""
        var smallIcon : Int = -1
        var largeIcon : Bitmap? = getBitmap(R.drawable.share_location_black)

        LocationDriver.configSetup?.let { driver ->
            title = driver.getNotificationTitle() ?: getString(R.string.notification_title_WARF, location_interval.toString())
            content = driver.getNotificationContent() ?: getString(R.string.notification_content_WARF, ActivityTransitionsUtil.toActivityString(activityType))
            smallIcon =if(driver.getNotificationSmallIcon()==-1){R.drawable.notifications_active}
            else{driver.getNotificationSmallIcon()}
            largeIcon =if(driver.getNotificationLargeIcon()==-1){
                getBitmap(R.drawable.share_location_black)
            }
            else{getBitmap(driver.getNotificationLargeIcon())}
        }

//        val intent = Intent(this,HomeActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//        intent.putExtra(Constants.CLICKED_ON_NOTIFICATION_KEY,true)
//        val pendingIntent = PendingIntent.getActivity(this,Constants.NOTIFICATION_PI_REQUEST_CODE,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this,Constants.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(smallIcon)
            .setLargeIcon(largeIcon)
//            .setContentIntent(pendingIntent)
            .setOngoing(false).setPriority(NotificationCompat.PRIORITY_HIGH)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(Constants.CHANNEL_ID,
                "Location fetching notificaiton",
                NotificationManager.IMPORTANCE_HIGH)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel)
            notification.setChannelId(Constants.CHANNEL_ID)

        }
        return notification.build()
    }

    private fun timeFormatter(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60

        val timeFormat = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        return timeFormat
    }
    fun timeNow():String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        if(hour>12){
            return String.format("%02d:%02d:%02d %s", hour-12, minute, second, amPm)
        }
       return String.format("%02d:%02d:%02d %s", hour, minute, second, amPm)
    }
}
