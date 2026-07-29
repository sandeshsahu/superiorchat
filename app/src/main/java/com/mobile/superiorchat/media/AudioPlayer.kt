package com.mobile.superiorchat.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import com.mobile.superiorchat.core.call.CallManager
import com.mobile.superiorchat.core.call.CallState

object AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted

    private val _progress = MutableStateFlow(0f) // 0.0 to 1.0
    val progress: StateFlow<Float> = _progress

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun play(context: Context, path: String) {
        if (_currentPlayingPath.value == path) {
            // Toggle play/pause
            if (mediaPlayer?.isPlaying == true) {
                pause()
            } else {
                resume()
            }
            return
        }

        // Prevent playing voice notes during a call
        if (CallManager.callState.value != CallState.IDLE) {
            return
        }

        // Stop current playing audio
        stop()

        val file = File(path)
        if (!file.exists()) return

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    _isCompleted.value = true
                    progressJob?.cancel()
                    _currentPositionMs.value = 0
                    _progress.value = 0f
                    // It is valid to call seekTo from PlaybackCompleted state
                    mediaPlayer?.seekTo(0)
                }
                prepare()
                start()
            }
            _currentPlayingPath.value = path
            _isPlaying.value = true
            _isCompleted.value = false
            _durationMs.value = mediaPlayer?.duration ?: 0
            startProgressUpdate()
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    private fun resume() {
        mediaPlayer?.start()
        _isPlaying.value = true
        _isCompleted.value = false
        startProgressUpdate()
    }

    fun pause() {
        mediaPlayer?.pause()
        _isPlaying.value = false
        progressJob?.cancel()
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
        val duration = _durationMs.value
        if (duration > 0) {
            _progress.value = positionMs.toFloat() / duration
        }
    }

    fun stop() {
        progressJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
        _isCompleted.value = false
        _currentPlayingPath.value = null
        _progress.value = 0f
        _currentPositionMs.value = 0
        _durationMs.value = 0
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition ?: 0
                val duration = mediaPlayer?.duration ?: 1
                _currentPositionMs.value = current
                _progress.value = current.toFloat() / duration
                delay(100)
            }
        }
    }
}
