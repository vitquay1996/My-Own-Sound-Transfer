package com.example.vitquay.myownsoundtranfer

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D
import kotlinx.android.synthetic.main.activity_receiver.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

/**
 * Created by Phung Tuan Hoang on 6/4/2018.
 */
class ReceiverActivity: Activity() {
    var soundSampler: SoundSampler? = null
    var fftThread: Thread? = null
    var fftArray: DoubleArray? = null
    var handler: Handler = Handler(Looper.getMainLooper())
    var monitorFreq: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_receiver)

        setupPermission()

        updateButton.setOnClickListener {
            if(soundSampler == null) {
                soundSampler = SoundSampler(samplingRateEditText.text.toString().toInt(),
                        bufferSizeEditText.text.toString().toInt())
                try {
                    soundSampler!!.init()
                } catch (e: Exception) {
                    Toast.makeText(this, "Sound Sample Init() Failed", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                Toast.makeText(this, "Sound Sampler Recording", Toast.LENGTH_LONG).show()

                fftArray = DoubleArray(soundSampler!!.bufferSize)

                val doubleFFT_1D = DoubleFFT_1D(soundSampler!!.bufferSize)

                fftThread = object : Thread() {
                    override fun run() {
                        while (true) {
                            shortArrayToDoubleArray(fftArray!!, soundSampler!!.buffer)
                            doubleFFT_1D.realForward(fftArray)
                            informListener(fftArray!!)
                            handler.post{
                                freqMonitorTextView.text = (Math.log((Math.sqrt(Math.pow(fftArray!![2 * monitorFreq], 2.0) + Math.pow(fftArray!![2 * monitorFreq + 1], 2.0)) ))).toString()
                            }
                            sleep(16)
                        }
                    }
                }
                fftThread!!.start()
            } else {
                fftThread!!.interrupt()
                soundSampler!!.stop()
                soundSampler = null
            }
        }

        addMonitorFreqButton.setOnClickListener {
            try {
                monitorFreq = addFreqEditText.text.toString().toInt()
            } catch (e: Exception) {
                Toast.makeText(this, "Error converting string to int", Toast.LENGTH_LONG).show();
            }
        }
    }

    fun shortArrayToDoubleArray(doubleArray: DoubleArray, shortArray: ShortArray) {
        if (shortArray.size != doubleArray.size) {
            println("Size mismatch")
            return
        } else {
            for (i in 0..shortArray.size - 1) {
                doubleArray[i] = shortArray[i].toDouble()
            }
        }
    }

    fun informListener(doubleArray: DoubleArray) {

    }

    private fun setupPermission() {
        val permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)

        if (permission != PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(this, "Failed to get audio recording permission", Toast.LENGTH_LONG).show()
            makeRequest();
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        if (requestCode == 1) {
            if (grantResults!!.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Audio recording permission denied by user", Toast.LENGTH_LONG).show();
            }
        }
    }
}