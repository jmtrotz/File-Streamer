package com.jmtrotz.filestreamer

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import com.pedro.encoder.input.decoder.AudioDecoderInterface
import com.pedro.encoder.input.decoder.VideoDecoderInterface
import com.pedro.rtplibrary.rtsp.RtspFromFile
import com.pedro.rtsp.utils.ConnectCheckerRtsp

import java.lang.Exception
import java.net.URLConnection

/**
 * Uses the RTSP library to stream the selected file.
 */
object FileStreamer: ConnectCheckerRtsp, AudioDecoderInterface, VideoDecoderInterface {
    private const val TAG = "FileStreamer"
    private val rtspFromFile: RtspFromFile = RtspFromFile(this, this, this)
    var message by mutableStateOf("")
    var isPreparingFile by mutableStateOf(false)
    var isStreaming by mutableStateOf(false)

    override fun onAuthSuccessRtsp() {
        Log.d(TAG, "Authorization successful")
        message = "Authorization Successful"
    }

    override fun onAuthErrorRtsp() {
        Log.e(TAG, "Authorization error")
        message = "Authorization Error"
        isStreaming = false
    }

    override fun onConnectionStartedRtsp(rtspUrl: String) {
        Log.d(TAG, "Connection started")
        message = "Connection Started"
    }

    override fun onConnectionSuccessRtsp() {
        Log.d(TAG, "Connection successful")
        message = "Connection Successful"
    }

    override fun onConnectionFailedRtsp(reason: String) {
        Log.e(TAG, "Connection failed")
        message = "Connection Failed"
        isStreaming = false
    }

    override fun onNewBitrateRtsp(bitrate: Long) {
        Log.d(TAG, "New bitrate")
        message = "New Bitrate"
    }

    override fun onDisconnectRtsp() {
        Log.d(TAG, "Disconnected")
        message = "Disconnected"
        isStreaming = false
    }

    override fun onAudioDecoderFinished() {
        Log.d(TAG, "Audio decoder finished")
        message = "Audio Decoder Finished"
    }

    override fun onVideoDecoderFinished() {
        Log.d(TAG, "Video decoder finished")
        message = "Video Decoder Finished"
    }

    /**
     * Starts file streaming.
     */
    fun startStream(filePath: String, url: String) {
        if (filePath.isNotBlank() && url.isNotBlank()) {
            try {
                if (prepareResult(filePath) && !isStreaming) {
                    Log.d(TAG, "Stream started")
                    message = "Stream Started"
                    isStreaming = true
                    rtspFromFile.startStream(url)
                } else {
                    Log.d(TAG, "Failed to prepare file")
                    message = "Error: Failed to prepare file"
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to start stream")
                message = "Error: Failed to start stream"
                exception.printStackTrace()
            }
        } else {
            Log.e(TAG, "Invalid input")
            message = "Error: File path or URL are invalid"
        }
    }

    /**
     * Stops file streaming.
     */
    fun stopStream() {
        Log.d(TAG, "Stream Stopped")
        message = "Stream Stopped"
        isStreaming = false
        rtspFromFile.stopStream()
    }

    /**
     * Prepares the file to be streamed.
     *
     * @param filePath Path to the file to be streamed.
     * @return True if it was able to prepare the file
     * for streaming, else false.
     */
    private fun prepareResult(filePath: String): Boolean {
        Log.d(TAG, "Preparing file")
        message = "Preparing file"
        isPreparingFile = true
        var result: Boolean?

        if (isVideo(filePath)) {
            Log.d(TAG, "File is a video")
            result = rtspFromFile.prepareVideo(filePath)
            result = result.or(rtspFromFile.prepareAudio(filePath))
        } else {
            Log.d(TAG, "File is not a video")
            result = rtspFromFile.prepareAudio(filePath)
        }

        isPreparingFile = false
        return result
    }

    /**
     * Checks if a file is a video or not.
     *
     * @param filePath Path to the file to check.
     * @return True if the file is a video, else false.
     */
    private fun isVideo(filePath: String): Boolean {
        Log.d(TAG, "Checking mime type")
        val mimeType = URLConnection.guessContentTypeFromName(filePath);
        return mimeType != null && mimeType.startsWith("video");
    }
}