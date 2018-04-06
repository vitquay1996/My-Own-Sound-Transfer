package com.example.vitquay.myownsoundtranfer

import android.media.AudioRecord
import android.util.Log
import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D

/**
 * Created by Phung Tuan Hoang on 15/3/2018.
 */

class SoundSampler(val FS: Int, var bufferSize: Int) {
    var audioRecord: AudioRecord? = null
    private val audioEncoding = 2
    private val nChannels = 16
    private var recordingThread: Thread? = null
    lateinit var buffer: ShortArray

    @Throws(Exception::class)
    fun init() {

        if (bufferSize < AudioRecord.getMinBufferSize(FS, nChannels, audioEncoding)) {
            Log.d("check BufferSize ", "Smaller than AudioRecord.getMinBufferSize")
            throw Exception()
        }

        try {
            if (audioRecord != null) {
                audioRecord!!.stop()
                audioRecord!!.release()
            }
            audioRecord = AudioRecord(1, FS, nChannels, audioEncoding,
                    bufferSize)
        } catch (e: Exception) {
            Log.d("Error in Init() ", e.message)
            throw Exception()
        }

        buffer = ShortArray(bufferSize)
        audioRecord!!.startRecording()

        recordingThread = object : Thread() {
            override fun run() {
                while (true) {
                    audioRecord!!.read(buffer, 0, bufferSize)
                }
            }
        }

        recordingThread!!.start()
        return
    }

    fun stop() {
        audioRecord!!.stop()
        audioRecord!!.release()
    }
}