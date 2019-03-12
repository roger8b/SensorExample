package br.com.rms.sensorexample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main2.*

class Main3Activity : AppCompatActivity() {

    lateinit var rotateScreen: RotateScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        rotateScreen = RotateScreen(this)

        rotateScreen.setView(img)



    }
}
