package br.com.rms.sensorexample

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.RotateAnimation
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class RotateScreen(val activity: AppCompatActivity) : LifecycleObserver {

    lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private var sensorEventListener: SensorEventListener? = null
    private var toDegree: Int = 0
    private var fromDegree: Int = 0
    private var view: View? = null

    var mOrientationDeg: Int = 0 //last rotation in degrees
    var mOrientationRounded: Int = 0 //last orientation int from above
    private val _DATA_X = 0
    private val _DATA_Y = 1
    private val _DATA_Z = 2
    private val ORIENTATION_UNKNOWN = -1

    init {
        activity.lifecycle.addObserver(this)
        verifySensor()
        initSensor()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun removeObserver() {
        activity.lifecycle.removeObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun verifySensor() {
        sensorManager =activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)

        sensorEventListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

            }

            private var tempOrientRounded: Int = 0
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
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

                    if (orientation != mOrientationDeg) {
                        mOrientationDeg = orientation
                        //figure out actual orientation
                        //Log.d("SENSOR_TEST", """Orientação $mOrientationDeg""")
                        if (orientation == -1) {//basically flat
                        } else if (orientation <= 45 || orientation > 315) {//round to 0
                            tempOrientRounded = 1//portrait
                        } else if (orientation > 45 && orientation <= 135) {//round to -90
                            tempOrientRounded = 2 //lsleft
                        } else if (orientation > 135 && orientation <= 255) {//round to -180
                            tempOrientRounded = 3 //upside down
                        } else if (orientation > 255 && orientation <= 315) {//round to 90 // 270
                            tempOrientRounded = 4//lsright
                        }
                    }
                }
                if (mOrientationRounded !== tempOrientRounded) {
                    //Orientation changed, handle the change here
                    mOrientationRounded = tempOrientRounded
                }
            }
        }
        checkOrientation()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun initSensor() {
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



    fun checkOrientation() {
        val subscription = Observable.interval(1000, 4000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                when (mOrientationRounded) {
                    1 -> {
                        toDegree = 0
                    }
                    2 -> {
                        toDegree = -90
                    }
                    3 -> {
                        toDegree = -180
                    }
                    4 -> {
                        toDegree = 90
                    }
                }
                rotate()
            }
    }

    public fun setView(view : View){
        this.view = view
    }



    private fun rotate() {
        checkOrientation()
        if(fromDegree == 90 && toDegree == -180){
            toDegree = 180
        }else if (fromDegree == 180 && toDegree == -180){
            toDegree = 180
        }else if(fromDegree == -180 && toDegree == 180){
            fromDegree = -180
        }

        Log.d("SENSOR_TEST", """Orientação De $fromDegree Para $toDegree""")
        val rotateAnimation = RotateAnimation(
            fromDegree.toFloat(),
            toDegree.toFloat(),
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f)

        rotateAnimation.duration = 500
        rotateAnimation.startOffset = 0L
        rotateAnimation.fillAfter = true
        rotateAnimation.interpolator = DecelerateInterpolator()
        view?.startAnimation(rotateAnimation)
        fromDegree = toDegree
    }


}