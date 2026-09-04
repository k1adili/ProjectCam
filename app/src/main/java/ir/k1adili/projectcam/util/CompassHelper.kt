package ir.k1adili.projectcam.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object CompassHelper {

    private val directionNames = arrayOf(
        "شمال", "شمال‌شرقی", "شرقی", "جنوب‌شرقی",
        "جنوب", "جنوب‌غربی", "غربی", "شمال‌غربی"
    )

    /**
     * Continuously streams the device's compass heading in degrees (0 = north, 90 = east,
     * 180 = south, 270 = west), remapped for whatever the device's current display rotation is
     * so the reading stays correct whether the phone is held in portrait or landscape.
     * Emits nothing (closes) if the device has no rotation-vector sensor.
     */
    fun observeHeadingDegrees(context: Context): Flow<Float> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensorManager == null || rotationSensor == null) {
            close()
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(9)
        val remappedMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val (axisX, axisY) = when (currentDisplayRotation(context)) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedMatrix)
                SensorManager.getOrientation(remappedMatrix, orientationAngles)

                val degrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                trySend((degrees + 360f) % 360f)
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    @Suppress("DEPRECATION")
    private fun currentDisplayRotation(context: Context): Int =
        if (Build.VERSION.SDK_INT >= 30) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
                ?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        }

    /** Nearest 8-point Persian cardinal/intercardinal direction label for a 0-360 heading. */
    fun directionLabel(degrees: Float): String {
        val normalized = ((degrees % 360f) + 360f) % 360f
        val index = ((normalized / 45f) + 0.5f).toInt() % 8
        return directionNames[index]
    }
}
