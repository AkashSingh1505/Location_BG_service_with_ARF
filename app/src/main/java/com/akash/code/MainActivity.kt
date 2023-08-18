package com.akash.code


import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.akash.location.LocationDriver


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var b = findViewById<Button>(R.id.button)
        var b2 = findViewById<Button>(R.id.button2)
        var b3 = findViewById<Button>(R.id.button3)
        var setup = LocationDriver.withARF()
            .setStartingTime(4,0,0)

            .install()


        b.setOnClickListener {
            setup.getPermission(this)
        }

        setup.getLocationCallback{longitude, latitude ->
            Toast.makeText(
                this@MainActivity,
                "Akash long : $longitude and latt : $latitude",
                Toast.LENGTH_SHORT
            ).show()

        }
        b2.setOnClickListener {

            setup.startService(this)

        }
        b3.setOnClickListener {

            setup.stopService()





        }

    }
}
