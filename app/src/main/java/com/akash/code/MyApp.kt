package com.akash.code

import android.app.Application
import com.akash.location.LocationDriver

class MyApp : Application(){
    override fun onCreate() {
        super.onCreate()
        LocationDriver.registerLocationDriver(this)
    }
}