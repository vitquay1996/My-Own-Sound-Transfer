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
    var lastTimeStringDecoded:Long = 0
    var prevDecodedString: String = ""
    var soundSampler: SoundSampler? = null
    var fftThread: Thread? = null
    var fftArray: DoubleArray? = null
    var handler: Handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_receiver)

        setupPermission()

        resetButton.setOnClickListener {
            peakTextView.text = ""
            prevDecodedString = ""
            lastTimeStringDecoded = System.currentTimeMillis()
        }

        soundSampler = SoundSampler(44100, 44100)
        try {
            soundSampler!!.init()
        } catch (e: Exception) {
            Toast.makeText(this, "Sound Sample Init() Failed", Toast.LENGTH_LONG).show()
        }

        fftArray = DoubleArray(soundSampler!!.bufferSize)

        val doubleFFT_1D = DoubleFFT_1D(soundSampler!!.bufferSize)

        fftThread = object : Thread() {
            override fun run() {
                while (true) {
                    if (Thread.currentThread().isInterrupted) {
                        return
                    }
                    shortArrayToDoubleArray(fftArray!!, soundSampler!!.buffer)
                    doubleFFT_1D.realForward(fftArray)
                    handler.post{
                        calculatePeak(500, 6000, 21)
                    }
                    try {
                        sleep(30)
                    } catch (e: InterruptedException) {
                        return
                    }
                }
            }
        }
        fftThread!!.start()
    }

    private fun calculatePeak(startFreq: Int, endFreq: Int, peakCount: Int) {
        var values = ArrayList<Pair>()

        for (i in startFreq .. endFreq) {
            var magnitude = Math.log((Math.sqrt(Math.pow(fftArray!![2 * i], 2.0) + Math.pow(fftArray!![2 * i + 1], 2.0)) ))
            values.add(Pair(i, magnitude))
        }

        values.sortDescending()

        var charArray = IntArray(peakCount)
        lateinit var pair:IntegerPair
//        for (i in 0 until peakCount) {
//            if (values[i].magnitude >15) {
//                pair = calculatePrimeCoefficients(13, 23, values[i].frequency)
//                if (pair.coeff1 < charArray.size) charArray[pair.coeff1] = pair.coeff2.toChar()
//            } else {
//                i--
//            }
//        }

        var i = 0
        var arrayCounter = 0

        while (i < peakCount && arrayCounter < values.size) {
            if (values[arrayCounter].magnitude > 15) {
                pair = calculatePrimeCoefficients(83, 23, values[arrayCounter].frequency)
                //println("A peak at " + values[arrayCounter].frequency.toString())
                if (pair.coeff1 < charArray.size) {
                    charArray[pair.coeff1] = pair.coeff2
                }
                i++
            }
            arrayCounter++
        }

        var sum = 0
        var average = 0f
        for ( i in 0 until charArray.size) {
            if (i != charArray.size - 1) {
                if (charArray[i] < 32 || charArray[i] > 126) return
                sum += charArray[i];
            }
            if (i == charArray.size - 1) {
                average = charArray[i].toFloat()
            }
        }

        if (average - (sum / (charArray.size - 1).toFloat()) < 0.001 && average != 0f) {
            handler.post {
                var secondCharArray  = CharArray(peakCount - 1)
                for ( i in 0 until secondCharArray.size) {
                    secondCharArray[i] = charArray[i].toChar()
                }
                var decodedString = String(secondCharArray)
                if (decodedString != prevDecodedString) {
                    peakTextView.text = peakTextView.text.toString() + decodedString
                    lastTimeStringDecoded = System.currentTimeMillis()
                    prevDecodedString = decodedString
                } else if (System.currentTimeMillis() - lastTimeStringDecoded > 1000) {
                    peakTextView.text = peakTextView.text.toString() + decodedString
                    lastTimeStringDecoded = System.currentTimeMillis()
                    prevDecodedString = decodedString
                }
            }
        }

    }

    private fun calculatePrimeCoefficients(prime1: Int, prime2: Int, number: Int): ReceiverActivity.IntegerPair {
        var i = 0

        while (i * prime1 <= number) {
            if ((number - i * prime1) % prime2 == 0) {
                return IntegerPair(i, (number - i * prime1) / prime2)
            } else {
                i++
            }
        }
        return IntegerPair(0, 0)
    }

    fun shortArrayToDoubleArray(doubleArray: DoubleArray, shortArray: ShortArray) {
        if (shortArray.size != doubleArray.size) {
            println("Size mismatch")
            return
        } else {
            for (i in 0 until shortArray.size) {
                doubleArray[i] = shortArray[i].toDouble()
            }
        }
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

    class Pair(var frequency: Int, var magnitude: Double) : Comparable<Pair> {
        override fun compareTo(other: Pair): Int {
            return when {
                this.magnitude > other.magnitude -> 1
                this.magnitude < other.magnitude -> -1
                else -> 0
            }
        }
    }

    class IntegerPair(var coeff1: Int, var coeff2: Int) {

    }

    override fun onDestroy() {
        fftThread!!.interrupt()

        try {
            soundSampler!!.stop()
        } catch (e: Exception) {
            Toast.makeText(this, "Sound Sample stop() Failed", Toast.LENGTH_LONG).show()
        }
        super.onDestroy()
    }
}