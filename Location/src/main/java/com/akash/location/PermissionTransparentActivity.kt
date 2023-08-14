package com.akash.location

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PermissionTransparentActivity : AppCompatActivity() {
    var isARFOn : Boolean=false
    lateinit var permissionManager: PermissionManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val myIntent= intent
        myIntent?.let {
             isARFOn = it.getBooleanExtra(Constants.ISARFON,false)
            permissionManager = PermissionManager(this,isARFOn)
            permissionManager.getPermissions()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            GPS_LOCATION_REQUEST_CODE -> {
                if (PermissionManager.isGpsEnable(this)) {
                    permissionManager.getPermissions()
                } else {
                    Log.i(TAG, "Please enable GPS")
                    permissionManager.showAllowDeviceLocationPopup()
                }
            }
            LOCATION_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (!PermissionManager.isPermissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION,this)) {
                        permissionManager.showBGLPopup()
                    } else {
                        permissionManager.getPermissions()
                    }
                } else {
                    if (!PermissionManager.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION,this) || !PermissionManager.isPermissionGranted(
                            Manifest.permission.ACCESS_COARSE_LOCATION,this)
                    ) {
                        permissionManager.showFineLocationPopup()
                    } else {
                        permissionManager.getPermissions()
                    }
                }
            }
            REQUEST_CODE_SYSTEM_ALERT_WINDOW -> {
                if (permissionManager.hasOverlayPermission()) {
                    permissionManager.getPermissions()
                } else {
                    permissionManager.showOverlayPermissionDeniedDialog()
                }
            }
            REQUEST_CODE_BETTERY_OPTIMIZATION -> {
                if (getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)) {
                    permissionManager.getPermissions()
                }
                else {
                    permissionManager.showBatteryPermissionDeniedDialog()
                }
            }
            REQUEST_CODE_AUTO_START ->{
                permissionManager.getPermissions()
            }
            REQUEST_CODE_GO_TO_SETTING ->{
                permissionManager.getPermissions()
            }
            GPS_ENABLE_REQUEST_CODE->{
                if(resultCode== Activity.RESULT_OK){
                    Log.i(TAG,"GPS is turned on")
                    permissionManager.getPermissions()

                }
                else if(resultCode== Activity.RESULT_CANCELED){
                    Log.i(TAG,"GPS is required to be turned on")
                    permissionManager.getPermissions()
                    Toast.makeText(this, "GPS is required to be turned on", Toast.LENGTH_SHORT).show()
                }
            }
            Constants.REQUEST_CODE_ACTIVITY_TRANSITION->{
                permissionManager.getPermissions()
            }

        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissionsArray: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissionsArray, grantResults)
        when(requestCode){
            Constants.REQUEST_CODE_ACTIVITY_TRANSITION->{
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionManager.getPermissions()
                } else {
                    permissionManager.showActivityRecognitionPermissionDeniedDialog()
                }
            }
        }
    }
}