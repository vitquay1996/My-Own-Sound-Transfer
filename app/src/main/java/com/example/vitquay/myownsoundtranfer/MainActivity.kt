package com.example.vitquay.myownsoundtranfer

import android.app.Activity
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.math.abs


class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button.setOnClickListener {
            val tone = generateTone("abc", 5)
            val enhancer = LoudnessEnhancer(tone.getAudioSessionId())
            enhancer.setTargetGain(10000)
            enhancer.setEnabled(true)
            tone.play()
            Toast.makeText(this, "Button pressed", Toast.LENGTH_LONG).show()
        }
        receiverButton.setOnClickListener {
            startActivity(Intent(this, ReceiverActivity::class.java))
        }
    }

    private fun generateTone(text: String, durationS: Int): AudioTrack {
        val count = (44100.0 * durationS).toInt()
        val samples = DoubleArray(count)
        val samples2 = DoubleArray(count)
        val samples3 = DoubleArray(count)

        var i = 2

         //For testing with pre-generated sine
        while (i < count) {
            var sample = Math.cos(2.0 * Math.PI * i.toDouble() / (44100.0 / 2000)) * 0x7FFF
            samples[i] = sample / 1.0
            i += 1
        }

        i = 0
        while (i < count) {
            var sample = Math.cos(2.0 * Math.PI * i.toDouble() / (44100.0 / 2181)) * 0x7FFF
            samples2[i] = sample / 1.0
            i += 1
        }
        i = 0
        while (i < count) {
            var sample = Math.cos(2.0 * Math.PI * i.toDouble() / (44100.0 / 2362)) * 0x7FFF
            samples3[i] = sample / 1.0
            i += 1
        }

//        // Convert String to array of frequency
//        val PRIME_1 = 13
//        val PRIME_2 = 23
//        var stringToFreqArray = IntArray(text.length)
//        for (item in text.indices) {
//            stringToFreqArray[item] = text[item].toByte().toInt()
//            Log.d("AADFDF", stringToFreqArray[item].toString())
//        }
//
//
//        while (i < count) {
//            if (i/2 == 450 * durationS) {
//                samples[i] = 0.0
//                samples[i + 1] = 900000.0
//            } else {
//                samples[i] = 0.0
//                samples[i+1] = 0.0
//            }
//            i += 2
//        }
//        val fftDo = DoubleFFT_1D(count)
        val buffer = ShortArray(count)
        val inverseBuffer = DoubleArray(count)
//
//        fftDo.realForward(samples)
//        fftDo.realForward(samples2)
//        for (i in 0..(count-1)) {
//            inverseBuffer[i] = (samples[i] + samples2[i])/2
//        }
//        Log.d("AAAA", samples[901].toString())
//        var max = 0.0
//        var maxIndex = -1
//        for (i in 0..(count-1)) {
//            if (abs(samples[i]) > max) {
//                max = samples[i]
//                maxIndex = i
//            }
//            buffer[i] = samples[i].toShort()
//        }
//        Log.d("AAAA", max.toString() + " " + maxIndex.toString())

//        fftDo.realInverse(inverseBuffer, true)
//        Log.d("AAAA", Arrays.toString(samples))
        for (i in 0..(count-1)) {
            inverseBuffer[i]= (samples[i] + samples2[i] + samples3[i]) / 3
        }
        for (i in 0..(count-1)) {
            buffer[i] = inverseBuffer[i].toShort()
        }

        val player = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_UNKNOWN)
                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        .build())
                .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(count * (16 / 8))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
//        val track = AudioTrack(AudioManager.STREAM_MUSIC, 44100,
//                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
//                count * (java.lang.Short.SIZE / 8), AudioTrack.MODE_STATIC)
        player.write(buffer, 0, count)
        player.setVolume(AudioTrack.getMaxVolume())
        return player
    }
}
