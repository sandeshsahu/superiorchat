/**
 * ─────────────────────────────────────────────────────────────
 *  Superiorchat Connect — Application Entry Point
 * ─────────────────────────────────────────────────────────────
 *  Wires the WebRTC manager ↔ UI controller ↔ Android bridge.
 *  This is the only module that imports both webrtc.js and ui.js.
 */

import rtc from './webrtc.js';
import ui from './ui.js';

// ─────────────────────────────────────────────────────────────
//  Android Bridge
// ─────────────────────────────────────────────────────────────

function notifyAndroid(action, data = '') {
    if (window.Android?.onWebRTCEvent) {
        window.Android.onWebRTCEvent(action, data);
    }
}

// ─────────────────────────────────────────────────────────────
//  URL Routing
// ─────────────────────────────────────────────────────────────

const hashString = window.location.hash.substring(1);
const params = new URLSearchParams(hashString);
const hostId = params.get('host');   // Android WebView: #host=UUID
const joinId = params.get('join');   // Telegram browser: #join=UUID
const secret = params.get('secret'); // Cryptographic call password

// ─────────────────────────────────────────────────────────────
//  Wire WebRTC Events → UI + Android Bridge
// ─────────────────────────────────────────────────────────────

rtc.on.ready = (peerId) => {
    if (rtc.isHost) {
        notifyAndroid('ready', peerId);
    } else {
        ui.setStatus('Connection ready. Tap to join.');
    }
};

rtc.on.connected = () => {
    if (rtc.isHost) {
        notifyAndroid('connected', '');
    } else {
        ui.setStatus('Connected');
    }
};

rtc.on.reconnecting = () => {
    if (rtc.isHost) {
        notifyAndroid('reconnecting', '');
    } else {
        ui.setStatus('Reconnecting...');
        ui.showToast('Network hiccup — reconnecting...');
    }
};

rtc.on.remoteVideo = (enabled) => {
    ui.setRemoteVideoVisible(enabled, rtc.isHost);
    // Auto-switch OS PiP between real video and audio canvas (no-op if PiP not active)
    ui.pip.syncRemoteVideoState(enabled);
    if (rtc.isHost) {
        notifyAndroid('remote_video', enabled ? 'on' : 'off');
    } else {
        ui.showToast(enabled ? 'Remote camera enabled' : 'Remote camera disabled');
    }
};

rtc.on.remoteFacingMode = (mode) => {
    ui.setRemoteFacingMode(mode);
};

rtc.on.stream = (remoteStream) => {
    ui.attachRemoteStream(remoteStream);
    // Hand a clone of the stream to the PiP controller so it
    // can display real video when the host has camera on
    ui.pip.attachRemoteStream(remoteStream);
    if (!rtc.isHost) ui.startTimer();
    ui.acquireWakeLock();
};

rtc.on.error = (msg) => {
    if (rtc.isHost) {
        notifyAndroid('error', msg);
    } else {
        ui.setStatus(msg);
    }
};

rtc.on.ended = () => {
    if (rtc.isHost) {
        notifyAndroid('ended', '');
    } else {
        const finalDuration = ui.durationText ? ui.durationText.textContent : '00:00';
        ui.showCallEnded(finalDuration);
    }
};

rtc.on.iceState = (state) => {
    console.log('[ICE]', state);
};

rtc.on.audioLevel = (level) => {
    ui.updateAudioLevel(level);
    // Feed level to PiP canvas so the avatar pulses to voice in OS PiP
    ui.pip.updateAudioLevel(level);
    if (rtc.isHost) {
        notifyAndroid('audio_level', level.toString());
    }
};

// Stream flipped (Camera swap)
rtc.on.localStreamFlipped = (stream, facingMode) => {
    ui.attachLocalStream(stream, facingMode);
};

// ─────────────────────────────────────────────────────────────
//  Wire UI Events → WebRTC
// ─────────────────────────────────────────────────────────────

// Join button (Guest only)
ui.btnJoin.addEventListener('click', async () => {
    // Synchronously warm up remote media element inside user gesture to satisfy autoplay policies
    if (ui.remoteVideoEl) ui.remoteVideoEl.play().catch(() => { });
    ui.setJoinLoading();

    try {
        const stream = await rtc.acquireMedia();
        if (!stream || !stream.getAudioTracks()[0] || stream.getAudioTracks()[0].readyState !== 'live') {
            ui.setJoinError('Microphone not ready. Please check permissions.');
            return;
        }
        ui.attachLocalStream(stream, rtc.currentFacingMode);

        rtc.placeCall(joinId, secret);
        ui.showActiveCallUI();
    } catch (err) {
        ui.setJoinError('Camera/Mic permission required');
    }
});

// Mute toggle
ui.btnMute.addEventListener('click', () => {
    const muted = rtc.toggleAudio();
    ui.updateMuteButton(muted);
});

// Video toggle
ui.btnVideo.addEventListener('click', () => {
    const videoOn = rtc.toggleVideo();
    ui.updateVideoButton(videoOn);
    ui.setLocalVideoVisible(videoOn);
    ui.showToast(videoOn ? 'Camera enabled' : 'Camera disabled');
});

// Flip Camera
if (ui.btnFlipCamera) {
    ui.btnFlipCamera.addEventListener('click', () => {
        // Trigger CSS animation
        ui.btnFlipCamera.classList.remove('flip-animate');
        void ui.btnFlipCamera.offsetWidth; // trigger reflow
        ui.btnFlipCamera.classList.add('flip-animate');
        rtc.flipCamera();
    });
}

// End call
ui.btnEnd.addEventListener('click', () => rtc.endCall());

// User confirmed leaving call via Exit Warning Modal
ui.onExitConfirmed(() => rtc.endCall());

// Minimize — enter OS-level Picture-in-Picture (Guest only)
// Host mode hides this button via CSS + enterHostMode(); guard is belt-and-suspenders.
if (ui.btnMinimize) {
    ui.btnMinimize.addEventListener('click', async () => {
        const entered = await ui.pip.open(rtc.remoteVideoOn);
        if (!entered) {
            ui.showToast('Picture-in-Picture is not supported by this browser.');
        }
    });
}

// ─────────────────────────────────────────────────────────────
//  Exposed Global Functions for Android WebView Bridge
// ─────────────────────────────────────────────────────────────

window.androidToggleMute = () => {
    return rtc.toggleAudio();
};

window.androidToggleVideo = () => {
    const videoOn = rtc.toggleVideo();
    ui.setLocalVideoVisible(videoOn);
    if (rtc.isHost) {
        notifyAndroid('local_video', videoOn ? 'on' : 'off');
    }
    return videoOn;
};

window.androidFlipCamera = () => {
    rtc.flipCamera();
};

window.androidToggleSwapVideo = () => {
    return ui.toggleVideoSwap();
};

window.androidEndCall = () => rtc.endCall();

window.androidSetPipMode = (isEnabled, targetVideo) => {
    // Security: strictly validate allowed values
    const validTargets = ['local', 'remote', 'none'];
    if (!validTargets.includes(targetVideo)) return;

    if (isEnabled) {
        document.body.classList.add('pip-mode');
        document.body.classList.remove('show-local-pip', 'show-remote-pip');
        
        if (targetVideo === 'local') {
            document.body.classList.add('show-local-pip');
        } else if (targetVideo === 'remote') {
            document.body.classList.add('show-remote-pip');
        }
    } else {
        document.body.classList.remove('pip-mode', 'show-local-pip', 'show-remote-pip');
    }
};

// ─────────────────────────────────────────────────────────────
//  Cleanup on Page Close
// ─────────────────────────────────────────────────────────────

ui.onPageUnload(() => rtc.endCall());

// ─────────────────────────────────────────────────────────────
//  Boot
// ─────────────────────────────────────────────────────────────

async function init() {
    const btnExit = document.getElementById('btnExit');
    if (btnExit) {
        btnExit.addEventListener('click', () => {
            if (window.Telegram?.WebApp) {
                try { window.Telegram.WebApp.close(); } catch (_) {}
            }
            window.close();
            // Fallback for browsers that block window.close()
            setTimeout(() => {
                btnExit.innerHTML = '<i class="fa-solid fa-circle-info"></i> Please close this tab manually';
                btnExit.disabled = true;
                btnExit.style.opacity = '0.7';
                btnExit.style.cursor = 'default';
            }, 300);
        });
    }

    if (hostId) {
        // ── Android App (Host) ───────────────────────────────
        rtc.isHost = true;
        ui.enterHostMode();

        try {
            const stream = await rtc.acquireMedia();
            ui.attachLocalStream(stream, rtc.currentFacingMode);
            rtc.setupPeer(hostId, secret);
            ui.showActiveCallUI();
            notifyAndroid('hardware_ready', '');
        } catch (err) {
            notifyAndroid('error', 'Media Error: ' + err.name + ' - ' + err.message);
        }

    } else if (joinId && secret) {
        // ── Telegram User (Guest) ────────────────────────────
        rtc.isHost = false;

        ui.setStatus('Validating secure link...');
        if (ui.btnJoin) ui.btnJoin.style.display = 'none';

        rtc.on.ready = () => {
            rtc.checkHostActive(joinId, secret, 5, (attempt, max) => {
                ui.setStatus(`Validating Link.. (Attempt ${attempt}/${max})`);
            }).then(isValid => {
                if (isValid) {
                    ui.setStatus('Connection ready. Tap to join.');
                    if (ui.btnJoin) ui.btnJoin.style.display = 'block';
                } else {
                    ui.showCallFailed();
                }
            });
        };

        rtc.setupPeer(null, secret);

    } else {
        ui.showInvalidLink();
    }
}

init();
