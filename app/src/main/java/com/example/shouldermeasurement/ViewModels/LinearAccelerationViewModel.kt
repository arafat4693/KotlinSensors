package com.example.shouldermeasurement.ViewModels

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.shouldermeasurement.MainApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.math.atan2
import kotlin.math.sqrt

class LinearAccelerationViewModel(
    private val sensorManager: SensorManager,
    private val context: Context
) : ViewModel() {
    private var linearAccelerationSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null

    // Angle data with timestamps
    private val angleData = mutableListOf<AngleDataPoint>()

    // State to hold current angle
    private val _algorithm1Angle = MutableStateFlow(0f) // EWMA (Algorithm 1)
    val algorithm1Angle: StateFlow<Float> = _algorithm1Angle

    private val _algorithm2Angle = MutableStateFlow(0f) // Sensor Fusion (Algorithm 2)
    val algorithm2Angle: StateFlow<Float> = _algorithm2Angle

    private var time = 0f

    // EWMA filter factor (adjustable for responsiveness)
    private val ewmaAlpha = 0.1f // Smaller values = smoother data

    // Complementary filter factor for Algorithm 2
    private val complementaryFilterAlpha = 0.98f

    // Track gyroscope integration
    private var gyroAngle = 0f
    private var lastGyroTimestamp: Long = 0

    // State to hold the previous filtered angle for EWMA
    private var previousFilteredAngle: Float = 0f

    // Listener for sensor data
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                when (it.sensor.type) {
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        val x = it.values[0]
                        val y = it.values[1]
                        val z = it.values[2]

                        // Calculate raw angle
                        val rawAngle = calculateAngle(x, y, z)

                        // Apply EWMA filter
                        val filteredAngle = ewmaFilter(rawAngle, previousFilteredAngle)
                        previousFilteredAngle = filteredAngle // Update the previous filtered value

                        // Update Algorithm 1 state
                        _algorithm1Angle.value = filteredAngle

                        // Combine with gyroscope for Algorithm 2
                        val fusedAngle = complementaryFilter(filteredAngle, gyroAngle)
                        _algorithm2Angle.value = fusedAngle

                        recordData(filteredAngle, fusedAngle)
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        // Integrate gyroscope angular velocity to get angle
                        updateGyroAngle(it)
                    }
                }
            }
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
    }

    init {
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    // Append data point during the measurement
    private fun recordData(algorithm1: Float, algorithm2: Float) {
        time += 0.05f // Increment time
        angleData.add(AngleDataPoint(time, algorithm1, algorithm2))
    }

    // Start collecting data
    fun startMeasurement() {
        angleData.clear()
        time = 0f

        linearAccelerationSensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        gyroscopeSensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_UI)
        }
    }

    // Stop collecting data
    fun stopMeasurement() {
        sensorManager.unregisterListener(sensorEventListener)
    }

    private fun calculateAngle(x: Float, y: Float, z: Float): Float {
        val norm = sqrt(x * x + y * y + z * z)
        val angle = atan2(y.toDouble(), norm.toDouble()).toDegrees()
        return angle.toFloat()
    }

    private fun ewmaFilter(input: Float, previousOutput: Float): Float {
        return ewmaAlpha * input + (1 - ewmaAlpha) * previousOutput
    }

    private fun updateGyroAngle(event: SensorEvent) {
        if(lastGyroTimestamp != 0L) {
            val dt = (event.timestamp - lastGyroTimestamp) / 1e-9f // Convert nanoseconds to seconds
            gyroAngle += event.values[1] * dt // Assuming Y-axis rotation
        }
        lastGyroTimestamp = event.timestamp
    }

    private fun complementaryFilter(linearAngle: Float, gyroAngle: Float): Float {
        return complementaryFilterAlpha * linearAngle + (1 - complementaryFilterAlpha) * gyroAngle
    }

    private fun Double.toDegrees() = Math.toDegrees(this)

    // Export data to CSV
    fun exportDataToCsv(): String? {
        val fileName = "shoulder_measurement_${System.currentTimeMillis()}.csv"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        try {
            FileWriter(file).use { writer ->
                // Write header
                writer.append("Time (s),Algorithm 1 (EWMA),Algorithm 2 (Fusion)\n")
                // Write data
                for (point in angleData) {
                    writer.append("${point.timestamp},${point.algorithm1Angle},${point.algorithm2Angle}\n")
                }
            }
            return file.absolutePath // Return the file path for sharing
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MainApplication)
                LinearAccelerationViewModel(
                    application.sensorManager,
                    application.applicationContext
                )
            }
        }
    }
}

data class AngleDataPoint(
    val timestamp: Float,
    val algorithm1Angle: Float,
    val algorithm2Angle: Float
)