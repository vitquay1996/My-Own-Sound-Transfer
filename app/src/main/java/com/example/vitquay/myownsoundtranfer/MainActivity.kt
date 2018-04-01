package com.example.vitquay.myownsoundtranfer

import android.app.Activity
import android.os.Bundle
import android.media.AudioTrack
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioFormat.CHANNEL_OUT_STEREO
import android.media.AudioManager
import android.media.AudioFormat.CHANNEL_OUT_STEREO
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.audiofx.LoudnessEnhancer
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
            val tone = generateTone(450.0, 1000)
            val enhancer = LoudnessEnhancer(tone.getAudioSessionId())
            enhancer.setTargetGain(10000)
            enhancer.setEnabled(true)
            tone.play()
            Toast.makeText(this, "Button pressed", Toast.LENGTH_LONG).show()
        }
    }

    private fun generateTone(freqHz: Double, durationMs: Int): AudioTrack {
        val count = (44100.0 * (durationMs / 1000.0)).toInt()
        val samples = DoubleArray(count)
        var i = 0

        // For testing with pre-generated sine
//        while (i < count) {
//            val sample = (Math.sin(2.0 * Math.PI * i.toDouble() / (44100.0 / freqHz)) * 0x7FFF)
//            samples[i] = sample
//            i += 1
//        }

        while (i < count) {
            if (i/2 == 450 || i/2 == 500) {
                samples[i] = 0.0
                samples[i + 1] = 9000000.0
            } else {
                samples[i] = 0.0
                samples[i+1] = 0.0
            }
            i += 2
        }
        val fftDo = DoubleFFT_1D(44100)
        val buffer = ShortArray(44100)

//        fftDo.realForward(samples)
//        Log.d("AAAA", samples[901].toString())
//        var max = 0.0
//        var maxIndex = -1
//        for (i in 0..(44100-1)) {
//            if (abs(samples[i]) > max) {
//                max = samples[i]
//                maxIndex = i
//            }
//            buffer[i] = samples[i].toShort()
//        }
//        Log.d("AAAA", max.toString() + " " + maxIndex.toString())

        fftDo.realInverse(samples, true)
        Log.d("AAAA", Arrays.toString(samples))
        for (i in 0..(44100-1)) {
            buffer[i] = samples[i].toShort()
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
