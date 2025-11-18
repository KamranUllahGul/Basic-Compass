package com.example.compass

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var compassDial: ImageView
    private lateinit var azimuthValue: TextView
    private lateinit var sensorManager: SensorManager
    private var accelerometerValues = FloatArray(3)
    private var magnetometerValues = FloatArray(3)
    private var currentDegree = 0f
    private val ALPHA = 0.15f
    private var smoothedAzimuth = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        compassDial = findViewById(R.id.compassDial)
        azimuthValue = findViewById(R.id.azimuthValue)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, magnet, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = lowPassFilter(event.values, accelerometerValues)
        }
        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetometerValues = lowPassFilter(event.values, magnetometerValues)
        }


        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerValues,
            magnetometerValues
        )
        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            var azimuthRadians = orientationAngles[0]
            var azimuth = Math.toDegrees(azimuthRadians.toDouble())

            if (azimuth < 0) {
                azimuth += 360
            }
            updateCompassNeedle(azimuth.toFloat())
        }
    }
    private fun lowPassFilter(input: FloatArray, output: FloatArray): FloatArray {
        val filtered = FloatArray(input.size)
        for (i in input.indices) {
            filtered[i] = output[i] + ALPHA * (input[i] - output[i])
        }
        return filtered
    }
    private fun updateCompassNeedle(azimuth: Float) {

        smoothedAzimuth = smoothAngle(smoothedAzimuth, azimuth, ALPHA)

        val rotate = RotateAnimation(
            currentDegree,
            -azimuth,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )

        rotate.duration = 200
        rotate.fillAfter = true

        compassDial.startAnimation(rotate)

        currentDegree = -azimuth
        // Update the TextView with azimuth and direction
        val direction = getDirection(azimuth)
        azimuthValue.text = "${azimuth.toInt()}° $direction"
    }
    private fun smoothAngle(oldAngle: Float, newAngle: Float, alpha: Float): Float {
        var diff = newAngle - oldAngle

        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360
        return oldAngle + alpha * diff
    }
    private fun getDirection(azimuth: Float): String {
        return when{
            azimuth >= 337.5 || azimuth < 22.5 -> "North"
            azimuth >= 22.5 && azimuth < 67.5 -> "North-East"
            azimuth >= 67.5 && azimuth < 112.5 -> "East"
            azimuth >= 112.5 && azimuth < 157.5 -> "South-East"
            azimuth >= 157.5 && azimuth < 202.5 -> "South"
            azimuth >= 202.5 && azimuth < 247.5 -> "South-West"
            azimuth >= 247.5 && azimuth < 292.5 -> "West"
            azimuth >= 292.5 && azimuth < 337.5 -> "North-West"
            else -> "North"
        }
    }

}





















