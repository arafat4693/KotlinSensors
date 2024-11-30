package com.example.shouldermeasurement

import android.app.Application
import android.hardware.SensorManager

class MainApplication : Application() {
    lateinit var sensorManager: SensorManager

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
    }
}