package com.akash.location

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import java.util.*


const val GRANTED = PackageManager.PERMISSION_GRANTED
const val GPS_ENABLE_REQUEST_CODE = 400
const val GPS_LOCATION_REQUEST_CODE = 101
const val GPS_LOCATION_REQUEST_CODE_SECOND = 102
const val LOCATION_REQUEST_CODE = 103
const val REQUEST_CODE_SYSTEM_ALERT_WINDOW = 104
const val REQUEST_CODE_BETTERY_OPTIMIZATION = 105
const val REQUEST_CODE_GO_TO_SETTING = 107
const val KEY_AUTO_START_CHECK = "auto_start_key"
const val PREF_NAME = "auto_start_shared_pref"
const val REQUEST_CODE_AUTO_START = 106


class PermissionManager(activity: Activity, isActivityRecognitionFeatureOn: Boolean) {
    private lateinit var context: PermissionTransparentActivity
    private var isARFOn: Boolean = false
    private var isDeniedLocationOnce = false

    init {
        this.context = activity as PermissionTransparentActivity
        isARFOn = isActivityRecognitionFeatureOn
    }


    fun showOverlayPermissionDeniedDialog() {
        AlertDialog.Builder(context).setTitle("Permission Required")
            .setMessage("To proceed, please grant the permission to show system-level dialogs.")
            .setPositiveButton("Grant Permission") { _, _ ->
                // Open the system settings page to allow the user to grant the permission
                openSystemSettingforOverlay()
            }.setCancelable(false).create().show()
    }

    fun showActivityRecognitionPermissionDeniedDialog() {
        AlertDialog.Builder(context).setTitle("Permission Required")
            .setMessage("Activity Recognition permission is required to improve battery life for location tracking. Please grant the permission from the Settings.")
            .setPositiveButton("Grant Permission") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                (context as Activity).startActivityForResult(
                    intent, Constants.REQUEST_CODE_ACTIVITY_TRANSITION
                )
            }.setCancelable(false).show()
    }

    fun showBatteryPermissionDeniedDialog() {
        AlertDialog.Builder(context).setTitle("Battery Optimization")
            .setMessage("This app needs to be whitelisted from battery optimization for location updates to work reliably.")
            .setPositiveButton("OK") { _, _ ->
                requestBatteryOptimizationPermission()
            }.setCancelable(false).show()
    }


    fun requestBatteryOptimizationPermission() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = Uri.parse("package:${context.packageName}")
        (context as Activity).startActivityForResult(intent, REQUEST_CODE_BETTERY_OPTIMIZATION)
    }

    fun hasBatteryOptimizationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getSystemService(PowerManager::class.java)
                .isIgnoringBatteryOptimizations(context.packageName)
        } else {
            return true
        }

    }

    private fun openSystemSettingforOverlay() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")
        )
        (context as Activity).startActivityForResult(intent, REQUEST_CODE_SYSTEM_ALERT_WINDOW)
    }

    fun hasActivityTransitionPermisision(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !ActivityTransitionsUtil.hasActivityTransitionPermissions(
                context
            )
        ) {
            return false
        }
        return true
    }

    private fun requestActivityTransitionPermission() {
        ActivityCompat.requestPermissions(
            context,
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
            Constants.REQUEST_CODE_ACTIVITY_TRANSITION
        )

    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }


    // Check if auto-start check has already been performed
    fun isAutoStartCheckPerformed(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean(KEY_AUTO_START_CHECK, false)
    }

    // Set auto-start check as performed
    private fun setAutoStartCheckPerformed(context: Context) {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putBoolean(KEY_AUTO_START_CHECK, true)
        editor.apply()
    }
    // for Xiaomi devices


    private fun requestAutostartPermission() {
        try {
            val manufacturer = Build.MANUFACTURER.toLowerCase(Locale.getDefault())
            val intent = Intent()

            when {
                manufacturer.contains("xiaomi") -> {
                    intent.component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                }

                manufacturer.contains("oppo") -> {
                    intent.component = ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                }

                manufacturer.contains("vivo") -> {
                    intent.component = ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpermissionManageranagerActivity"
                    )
                }

                manufacturer.contains("letv") -> {
                    intent.component = ComponentName(
                        "com.letv.android.letvsafe",
                        "com.letv.android.letvsafe.AutobootManageActivity"
                    )
                }

                manufacturer.contains("honor") || manufacturer.contains("huawei") -> {
                    intent.component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                }

                manufacturer.contains("realme") -> {
                    showAutoLaunchEnableDialog()
                    return
                }
            }

            val packageManager = context.packageManager
            val list: List<ResolveInfo> =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (list.isNotEmpty()) {
                showAutoStartPermissionDeniedDialog(intent)
            } else {
                setAutoStartCheckPerformed(context)
                getPermissions()
            }
        } catch (e: Exception) {
            Log.e("exc", e.toString())
        }
    }

    //this will be usefull for those devices in which we can't redirect the user to the setting page
    private fun showAutoLaunchEnableDialog() {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle("Enable Auto-launch for Your App")
        alertDialogBuilder.setMessage(
            R.string.enable_auto_launch_steps
        )
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.setPositiveButton("Go to Settings") { dialog, _ ->
            val intent = Intent(Settings.ACTION_SETTINGS)
            (context as Activity).startActivityForResult(intent, REQUEST_CODE_GO_TO_SETTING)
            setAutoStartCheckPerformed(context)
            dialog.dismiss()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }


    fun showAutoStartPermissionDeniedDialog(intent: Intent) {
        AlertDialog.Builder(context).setTitle("Autostart Permission?")
            .setMessage(R.string.auto_start_permission_step).setPositiveButton("OK") { _, _ ->
                // Open the system settings page to allow the user to grant the permission
                setAutoStartCheckPerformed(context)
                (context as Activity).startActivityForResult(intent, REQUEST_CODE_AUTO_START)
            }.setCancelable(false).show()
    }


    fun hasOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(context)) {
                return true
            }
            return false
        } else {
            return true
        }
    }

    fun showAllowDeviceLocationPopup() {
        AlertDialog.Builder(context)
            .setMessage("For a better experience, turn on device location, which uses Google's location service.")
            .setCancelable(false).setPositiveButton("OK") { _, _ ->
                goOnLocationSetting()
            }.show()
    }

    private val locationPermission =
        context.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when {
                    it.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                        Log.i(TAG, "Coarse location permission is granted")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            if (!isPermissionGranted(
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION, context
                                )
                            ) {
                                getBackgroundLocationPermission()
                            }
                        } else {
                            getPermissions()
                        }
                    }

                    it.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                        Log.i(TAG, "Fine location permission is granted")
                    }

                    true -> {
                        Log.i(TAG, "Location permission denied")
                        getAllLocationPermission()
                    }
                }

            }
        }

    fun getBackgroundLocationPermission() {
        backgroundLocation.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    private val backgroundLocation =
        context.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                Log.i(TAG, "Backgroundn location permission granted")
                getPermissions()
            } else {
                showBGLPopup()

            }
        }

    fun showBGLPopup() {
        AlertDialog.Builder(context).setTitle("Background Location Permission Required")
            .setMessage(R.string.bglocationText).setPositiveButton("Go to Settings") { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", context.packageName, null)
                (context as Activity).startActivityForResult(
                    intent, LOCATION_REQUEST_CODE
                )
            }.setCancelable(false).show()
    }

    private fun getAllLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            isDeniedLocationOnce = true
            AlertDialog.Builder(context).setTitle("Location Permission Required")
                .setMessage("This app requires access to your location to function properly. Please grant the location permission to continue.")
                .setPositiveButton("Grant Permission") { _, _ ->
                    // Request location permission again
                    locationPermission.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }.setCancelable(false)
//                .setNegativeButton("Cancel") { _, _ -> }
                .show()
            return
        } else {
            if (isDeniedLocationOnce) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    showBGLPopup()
                } else {
                    showFineLocationPopup()
                }
                return
            }
            locationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun showFineLocationPopup() {
        AlertDialog.Builder(context).setTitle("Location Permission Required")
            .setMessage("This app requires access to your location to function properly. Please manually enable the location permission in the app settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", context.packageName, null)
                (context as Activity).startActivityForResult(intent, LOCATION_REQUEST_CODE)

            }.setCancelable(false)
//                    .setNegativeButton("Cancel") { _, _ -> }
            .show()
    }


    private fun goOnLocationSetting() {
        var intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        (context as Activity).startActivityForResult(intent, GPS_LOCATION_REQUEST_CODE)
    }

    fun getPermissions() {
        if (isGpsEnable(context)) {
            if (haveLocationPermissionManager(context)) {
                Log.i(TAG, "All location PermissionManager have been granted")
                if (hasOverlayPermission()) {
                    Log.i(TAG, "Overlay  permisiion has granted")
                    if (hasBatteryOptimizationPermission()) {
                        Log.i(
                            TAG, "BatteryOptimization permission granted"
                        )
//                        // Check if autostart permission is granted
                        if (isAutoStartCheckPerformed(context)) {
                            Log.i(TAG, "autostart permission might be granted")
                            if (isARFOn) {
                                if (hasActivityTransitionPermisision()) {
                                    Log.i(TAG, "Activity Transaciton permission  has been granted")
                                    context.finish()
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        requestActivityTransitionPermission()
                                    }
                                }
                            } else {
                                context.finish()
                            }
                        } else {
                            requestAutostartPermission()
                        }
                    } else {
                        requestBatteryOptimizationPermission()
                    }
                } else {
                    showOverlayPermissionDeniedDialog()
                }
            } else {
                getAllLocationPermission()
            }
        } else {
            locationSettingRequest(context as Activity)
        }
    }


    companion object {

        fun haveLocationPermissionManager(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!isPermissionGranted(
                        Manifest.permission.ACCESS_FINE_LOCATION, context
                    ) || !isPermissionGranted(
                        Manifest.permission.ACCESS_COARSE_LOCATION, context
                    ) || !isPermissionGranted(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION, context
                    )
                ) {
                    return false
                }
            } else if (!isPermissionGranted(
                    Manifest.permission.ACCESS_FINE_LOCATION, context
                ) || !isPermissionGranted(
                    Manifest.permission.ACCESS_COARSE_LOCATION, context
                )
            ) {
                return false
            }
            return true
        }

        fun isPermissionGranted(permission: String, context: Context): Boolean {
            if (ActivityCompat.checkSelfPermission(context, permission) == GRANTED) {
                return true
            }
            return false
        }

        fun isGpsEnable(context: Context): Boolean {

            var locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }

        fun locationSettingRequest(activityContext: Activity) {
            val locationRequest =

                LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    LocationDriver.configSetup!!.getServiceStaticIntervalInMillis()
                ).build()
            val locationSettingRequest =
                LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
                    .setAlwaysShow(true).build()
            val task = LocationServices.getSettingsClient(activityContext)
                .checkLocationSettings(locationSettingRequest)
            task.addOnSuccessListener {
                Log.i(TAG, "Gps is already enable")

            }
            task.addOnFailureListener {
                when ((it as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            val resolvableApiException = it as ResolvableApiException
                            resolvableApiException.startResolutionForResult(
                                activityContext, GPS_ENABLE_REQUEST_CODE
                            )

                        } catch (ex: IntentSender.SendIntentException) {
                            ex.printStackTrace()
                        }
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        Log.d(TAG, "Device does not have locaiton")
                        // Device does not have location
                    }
                }
            }

        }
    }
}
