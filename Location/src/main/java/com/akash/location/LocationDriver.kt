package com.akash.location

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

interface LocationDriverConfigurator {
    fun setStartingTime(h: Int, m: Int, s: Int): LocationDriverConfigurator
    fun setStopTime(h: Int, m: Int, s: Int): LocationDriverConfigurator
    fun setInfoDialog(layout: Int, duration: Long): LocationDriverConfigurator
    fun setNotificationLargeIcon(icon: Int): LocationDriverConfigurator
    fun setNotificationSmallIcon(icon: Int): LocationDriverConfigurator
    fun setNotificationTitle(title: String?): LocationDriverConfigurator
    fun setNotificationContent(cont: String?): LocationDriverConfigurator
    fun install(): LocationDriver
}

interface ARFLocationDriverConfigurator : LocationDriverConfigurator {
    fun setDynamicLocationInterval(
        still: Long,
        walk: Long,
        run: Long,
        vehicle: Long,
        Unknown: Long
    ): ARFLocationDriverConfigurator
}

interface WithoutARFLocationDriverConfigurator : LocationDriverConfigurator {
    fun setStaticLocationIntervalInMillis(millis: Long): WithoutARFLocationDriverConfigurator
}

class LocationDriver private constructor(
    private val isARFOn: Boolean,
    private val still: Long,
    private val walk: Long,
    private val run: Long,
    private val vehicle: Long,
    private val Unknown: Long,
    private val sHour: Int,
    private val sMinutes: Int,
    private val sSecond: Int,
    private val eHour: Int,
    private val eMinutes: Int,
    private val eSecond: Int,
    private val infoDialogLayout: Int,
    private val infoDialogDuration: Long,
    private val notificationLargeIcon: Int,
    private val notificationSmallIcon: Int,
    private val notificationTitle: String?,
    private val notificationContent: String?,
    private val serviceStaticIntervalInMillis: Long
) {

    companion object ConfiguratorFactory {
        internal var configSetup: LocationDriver? = null
        internal var clientApplicationInstance: Application? = null
        fun registerLocationDriver(application: Application) {
            clientApplicationInstance = application
            clientApplicationInstance!!.registerActivityLifecycleCallbacks(
                LocationOverlayDialog.getInstance(
                    clientApplicationInstance!!.applicationContext
                )
            )
        }

        fun withARF(): ARFLocationDriverConfigurator = InstallerWithARF()
        fun withoutARF(): WithoutARFLocationDriverConfigurator = InstallerWithoutARF()
    }

    // ... other getter methods

    internal fun getIsARFOn(): Boolean = isARFOn

    internal fun getStill(): Long = still

    internal fun getWalk(): Long = walk

    internal fun getRun(): Long = run

    internal fun getVehicle(): Long = vehicle

    internal fun getUnknown(): Long = Unknown

    internal fun getSHour(): Int = sHour

    internal fun getSMinutes(): Int = sMinutes

    internal fun getSSecond(): Int = sSecond

    internal fun getEHour(): Int = eHour

    internal fun getEMinutes(): Int = eMinutes

    internal fun getESecond(): Int = eSecond

    internal fun getInfoDialogLayout(): Int = infoDialogLayout

    internal fun getInfoDialogDuration(): Long = infoDialogDuration

    internal fun getNotificationLargeIcon(): Int = notificationLargeIcon

    internal fun getNotificationSmallIcon(): Int = notificationSmallIcon

    internal fun getNotificationTitle(): String? = notificationTitle

    internal fun getNotificationContent(): String? = notificationContent

    internal fun getServiceStaticIntervalInMillis(): Long = serviceStaticIntervalInMillis


    fun getPermission(context: Context) {
        if (clientApplicationInstance != null) {
            var intent = Intent(context, PermissionTransparentActivity::class.java)
            intent.putExtra(Constants.ISARFON, isARFOn)
            context.startActivity(intent)
        } else {
            Log.e(
                "LocationDriver",
                "Unable to get Location_service_permissions, Please register 'LocationDriver"
            )

        }
    }

    fun startService(context: Context) {
        if (clientApplicationInstance != null) {
            LocationManager.runDriver(context)

        } else {
            Toast.makeText(
                context,
                "Error : Unable to start Location_service, Please register 'LocationDriver",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun stopService() {
        if (clientApplicationInstance != null) {
            LocationServiceDestroyWorker.destroySerivce()
        } else {
            Log.e(
                "LocationDriver",
                "Unable to stop Location_service, Please register 'LocationDriver"
            )
        }
    }

    fun getLocationCallback(callback: (longitude: Double, latitude: Double) -> Unit) {
        if (clientApplicationInstance != null) {
            LocationService.cordinates.observeForever { coordinate ->
                callback(coordinate.first, coordinate.second)
            }
        } else {
            Log.e(
                "LocationDriver",
                "Unable to get Callback of Location_service, Please register 'LocationDriver"
            )
        }
    }

    fun getActivityAndTransactionCallback(callback: (activity: String, transaction: String) -> Unit) {
        if (clientApplicationInstance != null) {
            if (isARFOn) {
                LocationService.activityAndTransition.observeForever { act_tran ->
                    callback(act_tran.first, act_tran.second)
                }
            } else {
                Toast.makeText(clientApplicationInstance, "Please on ARF first", Toast.LENGTH_LONG)
                    .show()
            }
        } else {
            Log.e(
                "LocationDriver",
                "Unable to get Callback of ActivityAndTransaction, Please register 'LocationDriver"
            )
        }
    }

    private class InstallerWithARF : ARFLocationDriverConfigurator {
        private var isARFOn: Boolean = true
        private var still: Long = 15 * 60 * 1000
        private var walk: Long = 10 * 60 * 1000
        private var run: Long = 5 * 60 * 1000
        private var vehicle: Long = 2 * 60 * 1000
        private var Unknown: Long = 8 * 60 * 1000
        private var sHour: Int = 0
        private var sMinutes: Int = 0
        private var sSecond: Int = 0
        private var eHour: Int = 0
        private var eMinutes: Int = 0
        private var eSecond: Int = 0
        private var infoDialogLayout: Int = -1
        private var infoPopupDuration: Long = 20000L
        private var notificationLargeIcon: Int = -1
        private var notificationSmallIcon: Int = -1
        private var serviceStaticIntervalInMillis: Long = 5 * 60 * 1000
        private var notificationTitle: String? = null
        private var notificationContent: String?= null



        override fun setDynamicLocationInterval(
            still: Long,
            walk: Long,
            run: Long,
            vehicle: Long,
            Unknown: Long
        ): ARFLocationDriverConfigurator =
            apply {
                this.still = still
                this.walk = walk
                this.run = run
                this.vehicle = vehicle
                this.Unknown = Unknown
            }

        override fun setStartingTime(h: Int, m: Int, s: Int): LocationDriverConfigurator =
            apply {
                sHour = h
                sMinutes = m
                sSecond = s
                if(eHour==0){
                    eHour=sHour+1
                }
            }

        override fun setStopTime(h: Int, m: Int, s: Int): LocationDriverConfigurator =
            apply {
                eHour = h
                eMinutes = m
                eSecond = s
            }

        override fun setInfoDialog(layout: Int, duration: Long): LocationDriverConfigurator =
            apply {
                infoDialogLayout = layout
                infoPopupDuration = duration
            }

        override fun setNotificationLargeIcon(icon: Int): LocationDriverConfigurator =
            apply { notificationLargeIcon = icon }

        override fun setNotificationSmallIcon(icon: Int): LocationDriverConfigurator =
            apply { notificationSmallIcon = icon }

        override fun setNotificationTitle(title: String?): LocationDriverConfigurator =
            apply {
                notificationTitle = if (title != null) {
                    title
                } else {
                    "Empty Title"
                }
            }

        override fun setNotificationContent(cont: String?): LocationDriverConfigurator =
            apply {
                notificationContent = if (cont != null) {
                    cont
                } else {
                    "Empty Content"
                }
            }

        override fun install(): LocationDriver {
            configSetup = LocationDriver(
                isARFOn,
                still,
                walk,
                run,
                vehicle,
                Unknown,
                sHour,
                sMinutes,
                sSecond,
                eHour,
                eMinutes,
                eSecond,
                infoDialogLayout,
                infoPopupDuration,
                notificationLargeIcon,
                notificationSmallIcon,
                notificationTitle,
                notificationContent,
                serviceStaticIntervalInMillis
            )
            return configSetup!!
        }
    }

    private class InstallerWithoutARF : WithoutARFLocationDriverConfigurator {
        private var still: Long = 15 * 60 * 1000
        private var walk: Long = 10 * 60 * 1000
        private var run: Long = 5 * 60 * 1000
        private var vehicle: Long = 2 * 60 * 1000
        private var Unknown: Long = 8 * 60 * 1000
        private var sHour: Int = 0
        private var sMinutes: Int = 0
        private var sSecond: Int = 0
        private var eHour: Int = sHour + 1
        private var eMinutes: Int = 0
        private var eSecond: Int = 0
        private var infoDialogLayout: Int = -1
        private var infoPopupDuration: Long = 20000L
        private var notificationLargeIcon: Int = -1
        private var notificationSmallIcon: Int = -1
        private var notificationTitle: String =
            clientApplicationInstance!!.getString(R.string.notification_title)
        private var notificationContent: String =
            clientApplicationInstance!!.getString(R.string.notification_content)
        private var serviceStaticIntervalInMillis: Long = 5 * 60 * 1000


        override fun setStaticLocationIntervalInMillis(millis: Long): WithoutARFLocationDriverConfigurator =
            apply { serviceStaticIntervalInMillis = millis }

        override fun setStartingTime(h: Int, m: Int, s: Int): LocationDriverConfigurator =
            apply {
                sHour = h
                sMinutes = m
                sSecond = s
            }

        override fun setStopTime(h: Int, m: Int, s: Int): LocationDriverConfigurator =
            apply {
                eHour = h
                eMinutes = m
                eSecond = s
            }

        override fun setInfoDialog(layout: Int, duration: Long): LocationDriverConfigurator =
            apply {
                infoDialogLayout = layout
                infoPopupDuration = duration
            }

        override fun setNotificationLargeIcon(icon: Int): LocationDriverConfigurator =
            apply { notificationLargeIcon = icon }

        override fun setNotificationSmallIcon(icon: Int): LocationDriverConfigurator =
            apply { notificationSmallIcon = icon }

        override fun setNotificationTitle(title: String?): LocationDriverConfigurator =
            apply {
                notificationTitle = if (title != null) {
                    title
                } else {
                    "Empty Title"
                }
            }

        override fun setNotificationContent(cont: String?): LocationDriverConfigurator =
            apply {
                notificationContent = if (cont != null) {
                    cont
                } else {
                    "Empty Content"
                }
            }

        override fun install(): LocationDriver {
            configSetup = LocationDriver(
                false, // isARFOn is set to false for this configuration
                still,
                walk,
                run,
                vehicle,
                Unknown,
                sHour,
                sMinutes,
                sSecond,
                eHour,
                eMinutes,
                eSecond,
                infoDialogLayout,
                infoPopupDuration,
                notificationLargeIcon,
                notificationSmallIcon,
                notificationTitle,
                notificationContent,
                serviceStaticIntervalInMillis
            )
            return configSetup!!
        }
    }
}
