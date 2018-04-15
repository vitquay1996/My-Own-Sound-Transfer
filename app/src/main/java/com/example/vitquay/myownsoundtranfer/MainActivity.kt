package com.example.vitquay.myownsoundtranfer

import android.app.Activity
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs


class MainActivity : Activity() {

    var transmitFlag = 0
    var numParts = 0
    lateinit var globalStringArray: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        text_box.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0 != null) {
                    num_char_text.setText("Text Length: " + p0.length.toString())
                }
            }
        })
        set_button.setOnClickListener {
            globalStringArray = splitString(text_box.text.toString())
            transmitFlag = 0
        }
        button.setOnClickListener {
            if (transmitFlag < numParts) {
                val tone = generateTone(globalStringArray.get(transmitFlag), 1)
                // If not loud enough
//            val enhancer = LoudnessEnhancer(tone.getAudioSessionId())
//            enhancer.setTargetGain(10000)
//            enhancer.setEnabled(true)
                tone.play()
            }
        }
        receiverButton.setOnClickListener {
            startActivity(Intent(this, ReceiverActivity::class.java))
        }
    }

    private fun splitString(text: String): ArrayList<String> {
        val arrayLength = text.length / 20 + 1
        Log.d("ASDF", "size is " + arrayLength.toString())
        var stringArray = ArrayList<String>()
        numParts = arrayLength
        for (i in 0..arrayLength-1) {
            if (i == arrayLength - 1) {
                stringArray.add(getStringOfLength(text.substring(i*20), 20))
            } else {
                stringArray.add(getStringOfLength(text.substring(i*20, (i+1) * 20), 20))
            }
        }
        return stringArray
    }

    private fun getStringOfLength(text: String, length: Int): String {
        if (text.length == length) {
            return text
        }
        val deficit = length - text.length
        var returnString = text
        for (i in 0..deficit-1) {
            returnString = returnString + " "
        }
        Log.d("ADFF", "String length is " + returnString.length)
        return returnString
    }

    private fun generateTone(text: String, durationS: Int): AudioTrack {
        val count = (44100.0 * durationS).toInt()
        val buffer = ShortArray(count)

        // Initialize inverseBuffer to all 0s
        val inverseBuffer = DoubleArray(count)
        for (i in 0..(count-1)) {
            inverseBuffer[i]= 0.0
        }

        // Convert String to array of frequency
        val PRIME_1 = 83
        val PRIME_2 = 23
        var stringToFreqArray = DoubleArray(text.length + 1)
        var asciiArray = DoubleArray(text.length)
        for (item in text.indices) {
            stringToFreqArray[item] = PRIME_1 * item + PRIME_2 * text[item].toByte().toDouble()
            asciiArray[item] = text[item].toByte().toDouble()
            Log.d("AADFDF", stringToFreqArray[item].toString())
        }
        val averageSum = asciiArray.average().toInt()

        stringToFreqArray[text.length] = PRIME_1 * text.length + PRIME_2 * averageSum.toDouble()
        Log.d("AADFDF", averageSum.toString() + " with frequency " + stringToFreqArray[text.length].toString())
        for (i in 0..text.length) {
            var j = 0
            while (j < count) {
                var sample = Math.cos(2.0 * Math.PI * j.toDouble() / (44100.0 / stringToFreqArray[i])) * 0x7FFF
                inverseBuffer[j] += sample / 1.0
                j += 1
            }
        }

        for (i in 0..count - 1) {
            inverseBuffer[i] = inverseBuffer[i] / (text.length + 1)
        }

        //Copy inverseBuffer to buffer
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

        player.write(buffer, 0, count)
        player.setVolume(AudioTrack.getMaxVolume())

        player.setNotificationMarkerPosition(count)
        player.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override  fun onPeriodicNotification(track: AudioTrack) {
                // nothing to do
            }

            override  fun onMarkerReached(track: AudioTrack) {
                Log.d("SSDFSDF", "Audio track end of file reached..." + transmitFlag)
                transmitFlag += 1
                button.performClick()
//                messageHandler.sendMessage(messageHandler.obtainMessage(PLAYBACK_END_REACHED))
            }
        })

        return player
    }
}
