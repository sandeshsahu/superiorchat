package com.mobile.superiorchat.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.os.Build
import com.mobile.superiorchat.media.AudioPlayer
import com.mobile.superiorchat.utils.AppLog
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.core.StatusFlow
import com.mobile.superiorchat.core.SyncState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

enum class CallState { IDLE, CONNECTING, ACTIVE, ENDING }

object CallManager {
    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState

    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var appContext: Context? = null
    
    private val _isSpeakerphoneOn = MutableStateFlow(false)
    val isSpeakerphoneOn: StateFlow<Boolean> = _isSpeakerphoneOn
    
    private var timerJob: Job? = null
    private var timeoutJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var currentRoomId: String? = null
        private set

    // =========================================================================
    // VERCEL DOMAIN
    // =========================================================================
    const val VERCEL_APP_URL = "https://superiorchat-connect.vercel.app"

    fun initCall(context: Context): Pair<String, String> {
        appContext = context.applicationContext
        setupHardware(context)
        AudioPlayer.stop()

        _callState.value = CallState.CONNECTING
        _callDuration.value = 0L

        val roomId = UUID.randomUUID().toString()
        currentRoomId = roomId

        // URL logic for Vercel WebRTC
        val vercelUrl = "$VERCEL_APP_URL/?host=$roomId"
        val telegramUrl = "$VERCEL_APP_URL/?join=$roomId"
        
        AppLog.log(LogCategory.SYSTEM, "Initiated PeerJS call with room $roomId")
        StatusFlow.reportStatus(SyncState.SUCCESS, "Secure Call Initiated")

        // 30-second timeout if not answered
        timeoutJob = scope.launch {
            delay(30000)
            if (_callState.value == CallState.CONNECTING) {
                AppLog.log(LogCategory.SYSTEM, "Call timeout - no answer")
                StatusFlow.reportStatus(SyncState.ERROR, "No answer")
                endCall()
            }
        }

        return Pair(vercelUrl, telegramUrl)
    }

    fun markConnected() {
        timeoutJob?.cancel()
        // Guard: If already ACTIVE, don't restart the timer (ICE reconnection fires this again)
        if (_callState.value == CallState.ACTIVE) {
            AppLog.log(LogCategory.SYSTEM, "Call re-confirmed connected (ICE recovered)")
            return
        }
        _callState.value = CallState.ACTIVE
        StatusFlow.reportStatus(SyncState.SUCCESS, "Call Active")
        AppLog.log(LogCategory.SYSTEM, "Call Active - PeerJS Connected")
        
        // Start duration timer (only once)
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                _callDuration.value += 1
            }
        }
    }

    fun endCall() {
        val wasActive = _callState.value == CallState.ACTIVE
        val finalDuration = _callDuration.value
        _callState.value = CallState.ENDING
        timeoutJob?.cancel()
        timerJob?.cancel()
        AppLog.log(LogCategory.SYSTEM, "Call Ended")
        if (wasActive) {
            StatusFlow.reportStatus(SyncState.IDLE, "Call Ended - ${finalDuration}s")
        } else {
            StatusFlow.reportStatus(SyncState.IDLE, "")
        }
        
        scope.launch {
            delay(1500)
            _callState.value = CallState.IDLE
            _callDuration.value = 0
            currentRoomId = null
            releaseHardware()
        }
    }

    private fun setupHardware(context: Context) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.isSpeakerphoneOn = false
        _isSpeakerphoneOn.value = false

        // Proximity Sensor
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "SuperiorChat:ProximityCall")
        
        proximitySensor?.let {
            sensorManager?.registerListener(proximityListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { _ -> }
                .build()
            audioManager?.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun releaseHardware() {
        audioManager?.mode = AudioManager.MODE_NORMAL
        audioManager?.isSpeakerphoneOn = false
        _isSpeakerphoneOn.value = false
        
        sensorManager?.unregisterListener(proximityListener)
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
        audioManager = null
    }

    fun toggleSpeaker() {
        audioManager?.let { am ->
            val newState = !am.isSpeakerphoneOn
            am.isSpeakerphoneOn = newState
            _isSpeakerphoneOn.value = newState
        }
    }

    private val proximityListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val distance = it.values[0]
                if (distance < (proximitySensor?.maximumRange ?: 5f)) {
                    // Close to ear
                    if (wakeLock?.isHeld == false) wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
                } else {
                    // Away from ear
                    if (wakeLock?.isHeld == true) wakeLock?.release()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
}
