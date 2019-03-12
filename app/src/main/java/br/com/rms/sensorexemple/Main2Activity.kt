package br.com.rms.sensorexemple

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main2.*
import java.util.concurrent.TimeUnit


class Main2Activity : AppCompatActivity() {

    lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var sensorEventListener: SensorEventListener? = null

    val UPSIDE_DOWN = 3
    val LANDSCAPE_RIGHT = 4
    val PORTRAIT = 1
    val LANDSCAPE_LEFT = 2
    var mOrientationDeg: Int = 0 //last rotation in degrees
    var mOrientationRounded: Int = 0 //last orientation int from above
    private val _DATA_X = 0
    private val _DATA_Y = 1
    private val _DATA_Z = 2
    private val ORIENTATION_UNKNOWN = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)

        sensorEventListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

            }

            private var tempOrientRounded: Int = 0


            override fun onSensorChanged(event: SensorEvent?) {


                if (event != null) {
                    Log.d("SENSOR_TEST", "Sensor Changed")
                    val values = event.values
                    var orientation = ORIENTATION_UNKNOWN
                    val X = -values[_DATA_X]
                    val Y = -values[_DATA_Y]
                    val Z = -values[_DATA_Z]
                    val magnitude = X * X + Y * Y
                    // Don't trust the angle if the magnitude is small compared to the y value
                    if (magnitude * 4 >= Z * Z) {
                        val OneEightyOverPi = 57.29577957855f
                        val angle = Math.atan2((-Y).toDouble(), X.toDouble()).toFloat() * OneEightyOverPi
                        orientation = 90 - Math.round(angle)
                        // normalize to 0 - 359 range
                        while (orientation >= 360) {
                            orientation -= 360
                        }
                        while (orientation < 0) {
                            orientation += 360
                        }
                    }

                    val subscription = Observable.interval(1000,5000, TimeUnit.MILLISECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe()
                            Log.d("SENSOR_TEST", "" + orientation * -1)

                            if (orientation != mOrientationDeg) {
                                mOrientationDeg = orientation
                                //figure out actual orientation
                                if (orientation == -1) {//basically flat

                                } else if (orientation <= 45 || orientation > 315) {//round to 0
                                    tempOrientRounded = 1//portrait
                                } else if (orientation > 45 && orientation <= 135) {//round to 90
                                    tempOrientRounded = 2 //lsleft
                                } else if (orientation > 135 && orientation <= 225) {//round to 180
                                    tempOrientRounded = 3 //upside down
                                } else if (orientation > 225 && orientation <= 315) {//round to 270
                                    tempOrientRounded = 4//lsright
                                }

                            }

                            if (mOrientationRounded !== tempOrientRounded) {
                                //Orientation changed, handle the change here
                                mOrientationRounded = tempOrientRounded
                                var o = 0
                                when (mOrientationRounded) {
                                    1 -> {
                                        img.rotation = 0f
                                        o = 0
                                    }
                                    2 -> {
                                        img.rotation = 90f
                                        o = 90
                                    }
                                    3 -> {
                                        img.rotation = 180f
                                        o = 180
                                    }
                                    4 -> {
                                        img.rotation = 270f
                                        o = 270
                                    }
                                }
                                Log.d("SENSOR_TEST", """Otientação $o""")

                            }


                        }

            }
        }


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
