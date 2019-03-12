package br.com.rms.sensorexample

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import io.reactivex.Observable
import kotlin.math.round
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    lateinit var sensorManager : SensorManager
    private var sensor : Sensor? = null
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var sensorEventListener : SensorEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrix(rotationMatrix, null,accelerometerReading,magnetometerReading)

        sensorEventListener = object : SensorEventListener{
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

            }

            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)

                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)

                    }
                }
            }

        }

        val subscription = Observable.interval(1000, 1000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe{
                updateOrientationAngles()
                Log.i("SENSOR_TEST", """AZIMUT ${round(Math.toDegrees(orientationAngles[0].toDouble()))}""")
                Log.i("SENSOR_TEST", """PITHC ${round(Math.toDegrees(orientationAngles[1].toDouble()))}""")
                Log.i("SENSOR_TEST", """ROLL ${round(Math.toDegrees(orientationAngles[2].toDouble()))}""")
            }

    }

    fun updateOrientationAngles() {
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

    }

    override fun onResume() {
        super.onResume()

        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                sensorEventListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                sensorEventListener,
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPanelClosed(featureId: Int, menu: Menu?) {
        super.onPanelClosed(featureId, menu)

        sensorManager.unregisterListener(sensorEventListener)
    }
}
