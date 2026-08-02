package com.mobile.superiorchat.core.call

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
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
import com.mobile.superiorchat.data.Prefs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import androidx.annotation.RequiresApi

enum class CallState { IDLE, CONNECTING, ACTIVE, ENDING }

enum class CallError { NONE, INVALID_URL, NETWORK_ERROR, NO_ANSWER, HARDWARE_ERROR }

/**
 * Singleton managing the full call lifecycle: state machine, audio hardware,
 * proximity sensor, and duration timer.
 *
 * State flow:  IDLE → CONNECTING → ACTIVE → ENDING → IDLE
 *
 * Usage:
 *   val (vercelUrl, telegramUrl) = CallManager.initCall(context)
 *   // ... WebView loads vercelUrl, JS bridge calls markConnected() ...
 *   CallManager.endCall()
 */
object CallManager {
    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState

    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration

    private val _lastCallFailedDueToError = MutableStateFlow(CallError.NONE)
    val lastCallFailedDueToError: StateFlow<CallError> = _lastCallFailedDueToError.asStateFlow()
        
    fun clearCallError() {
        _lastCallFailedDueToError.value = CallError.NONE
    }

    fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    fun formatDurationText(seconds: Long): String {
        if (seconds == 0L) return "0 seconds"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        
        val parts = mutableListOf<String>()
        if (h > 0) parts.add("$h hour${if (h > 1L) "s" else ""}")
        if (m > 0) parts.add("$m minute${if (m > 1L) "s" else ""}")
        if (s > 0) parts.add("$s second${if (s > 1L) "s" else ""}")
        
        return parts.joinToString(" ")
    }

    private val _isSpeakerphoneOn = MutableStateFlow(false)
    val isSpeakerphoneOn: StateFlow<Boolean> = _isSpeakerphoneOn

    private val _isVideoOn = MutableStateFlow(false)
    val isVideoOn: StateFlow<Boolean> = _isVideoOn

    private val _isRemoteVideoOn = MutableStateFlow(false)
    val isRemoteVideoOn: StateFlow<Boolean> = _isRemoteVideoOn

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var timerJob: Job? = null
    private var timeoutJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var currentRoomId: String? = null
        private set

    var currentSecret: String? = null
        private set

    var currentBaseUrl: String? = null
        private set

    /** Timeout before auto-ending an unanswered call. */
    private const val CALL_TIMEOUT_MS = 45_000L

    /** Grace period for the ENDING state before resetting to IDLE. */
    private const val ENDING_DELAY_MS = 1_500L

    // ─────────────────────────────────────────────────────────
    //  Call Lifecycle
    // ─────────────────────────────────────────────────────────

    data class ValidationResult(val url: String?, val networkFailed: Boolean)

    suspend fun findWorkingFallbackUrl(context: Context, urlsToTry: List<String>, isCallValidation: Boolean = false): ValidationResult {
        val roomId = UUID.randomUUID().toString()
        val secret = UUID.randomUUID().toString()
        var workingBaseUrl: String? = null
        var networkFailed = false

        for (baseUrl in urlsToTry) {
            val isValid = withContext(Dispatchers.IO) {
                try {
                    val url = java.net.URL("$baseUrl/call.html")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.requestMethod = "GET"
                    connection.connect()
                    
                    if (connection.responseCode == 200) {
                        val html = connection.inputStream.bufferedReader().use { it.readText() }
                        html.contains("<title>Superiorchat Connect</title>") || html.contains("id=\"ui-layer\"")
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                    val activeNetwork = cm.activeNetwork
                    val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
                    val hasInternet = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                    
                    if (!hasInternet) networkFailed = true
                    false
                }
            }
            
            if (isValid) {
                workingBaseUrl = baseUrl
                break
            }
            
            if (isCallValidation && (_callState.value == CallState.ENDING || _callState.value == CallState.IDLE)) {
                AppLog.log(LogCategory.SYSTEM, "Call cancelled by user during URL validation")
                return ValidationResult(null, false)
            }
        }

        if (workingBaseUrl == null && isCallValidation) {
            AppLog.log(LogCategory.SYSTEM, "WebRTC URL Validation Failed for all domains")
            markFailed(if (networkFailed) CallError.NETWORK_ERROR else CallError.INVALID_URL)
        }
        
        return ValidationResult(workingBaseUrl, networkFailed)
    }

    /**
     * Initiates a new call: generates room/secret, configures audio hardware,
     * and returns a pair of (hostUrl, guestUrl).
     */
    suspend fun initCall(context: Context): Pair<String, String>? {
        val prefs = Prefs.getInstance(context)
        val currentSavedUrl = prefs.webrtcBaseUrl.removeSuffix("/")
        val fallbackUrls = context.resources.getStringArray(com.mobile.superiorchat.R.array.webrtc_fallback_urls).map { it.removeSuffix("/") }.shuffled()
        
        val urlsToTry = if (currentSavedUrl.isNotEmpty() && !fallbackUrls.contains(currentSavedUrl)) {
            // Custom user-provided URL. Do not auto-fallback if it fails.
            listOf(currentSavedUrl)
        } else {
            // Default URL. Prioritize current, then fallback to remaining random defaults if limits are hit.
            (listOfNotNull(currentSavedUrl.takeIf { it.isNotEmpty() }) + fallbackUrls).distinct()
        }

        // Set state to CONNECTING before validation so the concurrency check works
        _callState.value = CallState.CONNECTING
        _callDuration.value = 0L

        // Generate call credentials early so we can use them in the validation ping.
        val roomId = UUID.randomUUID().toString()
        val secret = UUID.randomUUID().toString()
        
        val result = findWorkingFallbackUrl(context, urlsToTry, isCallValidation = true)
        val workingBaseUrl = result.url
        
        if (workingBaseUrl == null) {
            return null
        }
        
        // Save the working URL so it's prioritized next time
        if (workingBaseUrl != currentSavedUrl) {
            prefs.webrtcBaseUrl = workingBaseUrl
        }
        currentBaseUrl = workingBaseUrl

        setupHardware(context.applicationContext)
        AudioPlayer.stop()

        currentRoomId = roomId
        currentSecret = secret

        val vercelUrl = "$workingBaseUrl/call.html?host=$roomId&secret=$secret"
        val telegramUrl = "$workingBaseUrl/call.html?join=$roomId&secret=$secret"

        AppLog.log(LogCategory.SYSTEM, "Initiated PeerJS call with room $roomId on host $workingBaseUrl")
        StatusFlow.reportStatus(SyncState.SUCCESS, "Secure Call Initiated")

        return Pair(vercelUrl, telegramUrl)
    }

    /**
     * Starts the auto-timeout for ringing.
     * Should be called ONLY AFTER the join link is successfully delivered to Telegram.
     */
    fun startTimeout() {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(CALL_TIMEOUT_MS)
            if (_callState.value == CallState.CONNECTING) {
                AppLog.log(LogCategory.SYSTEM, "Call timeout - no answer")
                StatusFlow.reportStatus(SyncState.ERROR, "No answer")
                _lastCallFailedDueToError.value = CallError.NO_ANSWER
                endCall()
            }
        }
    }

    /**
     * Called by the JavaScript bridge when the WebRTC peer connection
     * is established. Starts the duration timer.
     */
    fun markConnected() {
        timeoutJob?.cancel()
        // Guard: If already ACTIVE, this is an ICE re-connection — don't restart timer
        if (_callState.value == CallState.ACTIVE) {
            com.mobile.superiorchat.utils.AppLog.log(LogCategory.SYSTEM, "Call re-confirmed connected (ICE recovered)")
            return
        }
        
        _callState.value = CallState.ACTIVE
        
        // Enforce audio route (Earpiece by default) right when WebRTC audio stream connects
        setSpeakerphone(_isSpeakerphoneOn.value)

        StatusFlow.reportStatus(SyncState.SUCCESS, "Call Active")
        AppLog.log(LogCategory.SYSTEM, "Call Active - PeerJS Connected")

        // Start duration timer (only once)
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                _callDuration.value += 1L
            }
        }
    }

    /**
     * Triggered by WebRTC error or timeout. End call but also signal failure for UI handling.
     */
    fun markFailed(error: CallError = CallError.INVALID_URL) {
        timeoutJob?.cancel()
        AppLog.log(LogCategory.SYSTEM, "Call Failed due to: $error")
        _lastCallFailedDueToError.value = error
        endCall()
    }

    /**
     * Ends the call gracefully. Transitions through ENDING → IDLE
     * with a brief delay for the UI "Call Ended" label to display.
     */
    fun endCall() {
        if (_callState.value == CallState.ENDING || _callState.value == CallState.IDLE) return

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
            delay(ENDING_DELAY_MS)
            _callState.value = CallState.IDLE
            _callDuration.value = 0
            currentRoomId = null
            currentSecret = null
            releaseHardware()
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Audio Controls
    // ─────────────────────────────────────────────────────────

    fun setSpeakerphone(enabled: Boolean) {
        val am = audioManager ?: return
        _isSpeakerphoneOn.value = enabled

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: Use modern AudioManager routing exclusively.
            if (enabled) {
                val devices = am.availableCommunicationDevices
                val targetDevice = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                if (targetDevice != null) {
                    am.setCommunicationDevice(targetDevice)
                    AppLog.log(LogCategory.SYSTEM, "Audio routed → Speaker")
                }
            } else {
                // Do NOT call clearCommunicationDevice() — that defaults to earpiece and ignores Bluetooth.
                // Instead, explicitly select the best available non-speaker device.
                selectBestNonSpeakerDevice(am)
            }
        } else {
            // Android 11 and below: Use legacy flags.
            if (enabled) {
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = true
                AppLog.log(LogCategory.SYSTEM, "Audio routed → Speaker (legacy)")
            } else {
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = false
                // Activate Bluetooth SCO if a BT device is paired and available.
                @Suppress("DEPRECATION")
                if (am.isBluetoothScoAvailableOffCall || am.isBluetoothA2dpOn) {
                    @Suppress("DEPRECATION")
                    am.startBluetoothSco()
                    @Suppress("DEPRECATION")
                    am.isBluetoothScoOn = true
                    AppLog.log(LogCategory.SYSTEM, "Audio routed → Bluetooth SCO (legacy)")
                } else {
                    AppLog.log(LogCategory.SYSTEM, "Audio routed → Earpiece (legacy)")
                }
            }
        }

        // If Speakerphone is ON → release proximity wake lock so screen stays ON.
        // Proximity sensor suppression when speaker is active is handled in proximityListener.
        if (enabled && wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    fun toggleSpeaker() {
        setSpeakerphone(!_isSpeakerphoneOn.value)
    }

    fun setLocalVideoState(isOn: Boolean) {
        _isVideoOn.value = isOn
    }

    fun setRemoteVideoState(isOn: Boolean) {
        _isRemoteVideoOn.value = isOn
    }

    // ─────────────────────────────────────────────────────────
    //  Hardware — Audio focus, proximity sensor, wake lock
    // ─────────────────────────────────────────────────────────

    private fun setupHardware(context: Context) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION

        // Default audio route to EARPIECE (ear mode by default)
        setSpeakerphone(false)

        // Proximity sensor → screen off when near ear
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
            "SuperiorChat:ProximityCall"
        )
        proximitySensor?.let {
            sensorManager?.registerListener(proximityListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Request audio focus
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager?.clearCommunicationDevice()
        } else {
            // Stop any active Bluetooth SCO session before releasing hardware
            @Suppress("DEPRECATION")
            audioManager?.stopBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager?.isBluetoothScoOn = false
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = false
        }
        audioManager?.mode = AudioManager.MODE_NORMAL
        _isSpeakerphoneOn.value = false
        _isVideoOn.value = false
        _isRemoteVideoOn.value = false

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

    /**
     * Android 12+: Selects the best available non-speaker audio device.
     * Priority: Bluetooth SCO → Bluetooth A2DP → Wired Headset → Earpiece → system default.
     *
     * This is called by setSpeakerphone(false) instead of clearCommunicationDevice(),
     * which would silently ignore Bluetooth and default to earpiece.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun selectBestNonSpeakerDevice(am: AudioManager) {
        val devices = am.availableCommunicationDevices

        // AudioDeviceInfo type constants used as ints to avoid toolchain resolution issues:
        // TYPE_BT_SCO = 7, TYPE_BT_A2DP = 8 (stable Android constants since API 23)
        val btSco  = 7 // AudioDeviceInfo.TYPE_BT_SCO
        val btA2dp = 8 // AudioDeviceInfo.TYPE_BT_A2DP

        val selected = devices.firstOrNull { it.type == btSco }
            ?: devices.firstOrNull { it.type == btA2dp }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }

        if (selected != null) {
            am.setCommunicationDevice(selected)
            val isBluetooth = selected.type == btSco || selected.type == btA2dp
            val isWired = selected.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                || selected.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            val label = if (isBluetooth) "Bluetooth (${selected.productName})"
                else if (isWired) "Wired Headset"
                else "Earpiece"
            AppLog.log(LogCategory.SYSTEM, "Audio routed → $label")
        } else {
            // No specific device found — let system decide
            am.clearCommunicationDevice()
            AppLog.log(LogCategory.SYSTEM, "Audio routed → system default")
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Proximity Sensor Listener
    // ─────────────────────────────────────────────────────────

    private val proximityListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                // If Speakerphone is enabled, suppress proximity screen-off wake lock
                if (_isSpeakerphoneOn.value) {
                    if (wakeLock?.isHeld == true) wakeLock?.release()
                    return
                }

                val distance = it.values[0]
                val threshold = proximitySensor?.maximumRange ?: 5f
                if (distance < threshold) {
                    // Close to ear → turn screen off (Earpiece mode only)
                    if (wakeLock?.isHeld == false) wakeLock?.acquire(10 * 60 * 1000L)
                } else {
                    // Away from ear → turn screen on
                    if (wakeLock?.isHeld == true) wakeLock?.release()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
}
