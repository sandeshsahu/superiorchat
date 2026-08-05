/**
 * ─────────────────────────────────────────────────────────────
 *  Superiorchat Connect — UI Controller
 * ─────────────────────────────────────────────────────────────
 *  Owns every DOM interaction: status labels, timer, button
 *  states, video element visibility, toast notifications,
 *  screen wake lock, and page lifecycle management.
 *
 *  This module never imports webrtc.js — it is purely
 *  presentational. The app.js wires events between them.
 */

// ── PipController — OS-level Picture-in-Picture (guest only) ──
//
//  Strategy:
//  - One hidden <video id="pipVideo"> is the single PiP target.
//  - An off-screen <canvas id="pipCanvas"> draws the audio visualizer at 30fps.
//  - `pipVideo.srcObject` is dynamically updated between the live camera feed
//    and `canvas.captureStream(30)`.
//  - Changing `srcObject` on an active PiP element updates the floating window
//    instantly on both Chrome and Safari without needing extra user gestures.

class PipController {
    constructor() {
        // Create Canvas element (Attached to DOM as 1px so WebKit compositor renders captureStream)
        this._canvas = document.createElement('canvas');
        this._canvas.id = 'pipCanvas';
        this._canvas.width = 360; // 9:16 portrait ratio
        this._canvas.height = 640;
        this._canvas.style.position = 'fixed';
        this._canvas.style.bottom = '0';
        this._canvas.style.right = '0';
        this._canvas.style.width = '1px';
        this._canvas.style.height = '1px';
        this._canvas.style.opacity = '0.01';
        this._canvas.style.pointerEvents = 'none';
        this._canvas.style.zIndex = '-9999';
        document.body.appendChild(this._canvas);

        this._ctx = this._canvas.getContext('2d');

        // Create PiP Video element (The SINGLE target element for Picture-in-Picture)
        this._pipVideo = document.createElement('video');
        this._pipVideo.id = 'pipVideo';
        this._pipVideo.autoplay = true;
        this._pipVideo.muted = true;
        this._pipVideo.playsInline = true;
        this._pipVideo.style.position = 'fixed';
        this._pipVideo.style.bottom = '0';
        this._pipVideo.style.right = '0';
        this._pipVideo.style.width = '1px';
        this._pipVideo.style.height = '1px';
        this._pipVideo.style.opacity = '0.01';
        this._pipVideo.style.pointerEvents = 'none';
        this._pipVideo.style.zIndex = '-9999';

        if ('autoPictureInPicture' in HTMLVideoElement.prototype) {
            this._pipVideo.autoPictureInPicture = true;
        }
        document.body.appendChild(this._pipVideo);

        this._audioLevel = 0;
        this._isInPip = false;
        this._isRemoteVideoOn = false;
        this._remoteStream = null;
        this._canvasStream = this._canvas.captureStream(30);
        this._animFrame = null;

        // PiP Exit Event Listeners (Dual path for Chrome and iOS WebKit)
        this._pipVideo.addEventListener('leavepictureinpicture', () => this._onPipExit());
        this._pipVideo.addEventListener('webkitpresentationmodechanged', () => {
            if (this._pipVideo.webkitPresentationMode === 'inline') {
                this._onPipExit();
            }
        });
    }

    /** Call when the remote WebRTC stream arrives */
    attachRemoteStream(stream) {
        this._remoteStream = stream;
        if (this._isInPip) {
            this._applySource();
        }
    }

    /**
     * Open PiP immediately (User Gesture).
     * Supports both W3C requestPictureInPicture and iOS WebKit webkitSetPresentationMode on _pipVideo.
     * @param {boolean} isRemoteVideoOn — current camera state of the host
     */
    async open(isRemoteVideoOn) {
        if (!this._pipVideo) return false;
        this._isRemoteVideoOn = isRemoteVideoOn;
        this._isInPip = true;

        // Attach source & start canvas draw loop only when PiP is launching
        this._applySource();
        this._startCanvasLoop();

        try {
            await this._pipVideo.play();
        } catch (_) { }

        // Path 1: Standard W3C PiP API (Chrome / Edge / Firefox / Desktop Safari)
        if (document.pictureInPictureEnabled && typeof this._pipVideo.requestPictureInPicture === 'function') {
            try {
                await this._pipVideo.requestPictureInPicture();
                return true;
            } catch (err) {
                console.warn('[PiP] W3C requestPictureInPicture error:', err);
            }
        }

        // Path 2: iOS WebKit Prefix API (iOS Safari, Telegram iOS WebView)
        if (typeof this._pipVideo.webkitSupportsPresentationMode === 'function') {
            if (this._pipVideo.webkitSupportsPresentationMode('picture-in-picture')) {
                try {
                    this._pipVideo.webkitSetPresentationMode('picture-in-picture');
                    return true;
                } catch (err) {
                    console.warn('[PiP] WebKit webkitSetPresentationMode error:', err);
                }
            }
        } else if (typeof this._pipVideo.webkitSetPresentationMode === 'function') {
            try {
                this._pipVideo.webkitSetPresentationMode('picture-in-picture');
                return true;
            } catch (err) {
                console.warn('[PiP] WebKit direct webkitSetPresentationMode error:', err);
            }
        }

        // If launching PiP failed, clean up state immediately
        this._onPipExit();
        return false;
    }

    /** Explicitly close PiP window on Chrome and iOS Safari */
    close() {
        if (document.pictureInPictureElement) {
            document.exitPictureInPicture().catch(() => { });
        }
        if (this._pipVideo) {
            if (this._pipVideo.webkitPresentationMode === 'picture-in-picture') {
                try {
                    this._pipVideo.webkitSetPresentationMode('inline');
                } catch (_) { }
            }
        }
        this._onPipExit();
    }

    /** Called when host toggles their camera. Seamlessly updates active PiP feed. */
    syncRemoteVideoState(enabled) {
        this._isRemoteVideoOn = enabled;
        if (!this._isInPip) return;
        this._applySource();
    }

    /** Feed the audio level from WebRTC analyser to the canvas visualizer. */
    updateAudioLevel(level) {
        this._audioLevel = level;
    }

    // ── Private ──────────────────────────────────────────

    /** Cleanup method when PiP window closes */
    _onPipExit() {
        this._isInPip = false;
        if (this._pipVideo) {
            this._pipVideo.srcObject = null;
            this._pipVideo.pause();
        }
        this._stopCanvasLoop();
    }

    /** Swaps pipVideo.srcObject on the active PiP window based on current camera state */
    _applySource() {
        if (!this._pipVideo) return;

        const remoteVideoEl = document.getElementById('remoteVideo');
        const remoteStream = this._remoteStream || (remoteVideoEl ? remoteVideoEl.srcObject : null);

        const targetStream = (this._isRemoteVideoOn && remoteStream)
            ? remoteStream
            : this._canvasStream;

        if (this._pipVideo.srcObject !== targetStream) {
            this._pipVideo.srcObject = targetStream;
            this._pipVideo.play().catch(() => { });
        }
    }

    /** Continuous 30fps canvas rendering loop (Only runs while in PiP) */
    _startCanvasLoop() {
        if (this._animFrame) return; // Already running
        const draw = () => {
            if (!this._isInPip) {
                this._stopCanvasLoop();
                return;
            }
            this._animFrame = requestAnimationFrame(draw);
            this._drawCanvas(this._audioLevel);
        };
        draw();
    }

    /** Stop canvas draw loop when PiP closes */
    _stopCanvasLoop() {
        if (this._animFrame) {
            cancelAnimationFrame(this._animFrame);
            this._animFrame = null;
        }
    }

    /** Feed the audio level from WebRTC analyser to the canvas visualizer. */
    updateAudioLevel(level) {
        this._audioLevel = level;
    }

    // ── Private ──────────────────────────────────────────

    /** Continuous 30fps canvas rendering loop */
    _startCanvasLoop() {
        const draw = () => {
            requestAnimationFrame(draw);
            this._drawCanvas(this._audioLevel);
        };
        draw();
    }

    /** Draw audio visualizer frame */
    _drawCanvas(level) {
        if (!this._ctx) return;
        const ctx = this._ctx;
        const W = this._canvas.width;    // 360
        const H = this._canvas.height;   // 640
        const cx = W / 2;
        const cy = H / 2 - 30;

        // 1. Dark App Background (#0f172a)
        ctx.fillStyle = '#0f172a';
        ctx.fillRect(0, 0, W, H);

        // 2. Radial Ambient Glow
        const bgGlow = ctx.createRadialGradient(cx, cy, 0, cx, cy, W * 0.8);
        bgGlow.addColorStop(0, 'rgba(124, 58, 237, 0.25)');
        bgGlow.addColorStop(1, 'rgba(15, 23, 42, 0)');
        ctx.fillStyle = bgGlow;
        ctx.fillRect(0, 0, W, H);

        // 3. Audio Pulsing Glow & Circles
        const baseR = 55;
        const pulseR = baseR + level * 35;
        const alpha = Math.min(1, 0.3 + level * 0.7);

        // Outer Glow Ring
        ctx.beginPath();
        ctx.arc(cx, cy, pulseR + 25, 0, Math.PI * 2);
        ctx.strokeStyle = `rgba(124, 58, 237, ${alpha * 0.4})`;
        ctx.lineWidth = 2;
        ctx.stroke();

        // Inner Pulsing Circle
        ctx.beginPath();
        ctx.arc(cx, cy, pulseR, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(124, 58, 237, ${0.8 + level * 0.2})`;
        ctx.fill();

        // 4. Center Avatar Silhouette / Icon
        ctx.beginPath();
        ctx.arc(cx, cy - 10, 18, 0, Math.PI * 2);
        ctx.fillStyle = '#ffffff';
        ctx.fill();

        ctx.beginPath();
        ctx.arc(cx, cy + 32, 28, Math.PI, 0, false);
        ctx.fillStyle = '#ffffff';
        ctx.fill();

        // 5. Text Label at Bottom
        ctx.font = '600 16px system-ui, -apple-system, sans-serif';
        ctx.fillStyle = '#94a3b8';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'bottom';
        ctx.fillText('Superiorchat Connect', cx, H - 40);

        ctx.font = '500 13px system-ui, -apple-system, sans-serif';
        ctx.fillStyle = '#64748b';
        ctx.fillText('End-to-End Encrypted', cx, H - 20);
    }
}

class UIController {
    constructor() {
        // ── DOM References ────────────────────────────────────
        this.statusText = document.getElementById('statusText');
        this.durationText = document.getElementById('durationText');
        this.avatarContainer = document.getElementById('avatarContainer');
        this.avatarPulse = document.querySelector('.avatar-pulse');
        this.btnJoin = document.getElementById('btnJoin');
        this.activeControls = document.getElementById('activeControls');
        this.remoteVideoEl = document.getElementById('remoteVideo');
        this.localVideoEl = document.getElementById('localVideo');
        this.uiLayer = document.getElementById('ui-layer');
        this.btnMute = document.getElementById('btnMute');
        this.btnVideo = document.getElementById('btnVideo');
        this.btnFlipCamera = document.getElementById('btnFlipCamera');
        this.flipContainer = document.getElementById('flipContainer');
        this.btnEnd = document.getElementById('btnEnd');
        this.btnMinimize = document.getElementById('btnMinimize');
        this.minimizeContainer = document.getElementById('minimizeContainer');
        this.lblMute = document.getElementById('lblMute');
        this.lblVideo = document.getElementById('lblVideo');
        this.toastContainer = document.getElementById('toastContainer');
        this.networkBanner = document.getElementById('networkBanner');
        this.exitModal = document.getElementById('exitModal');
        this.btnCancelExit = document.getElementById('btnCancelExit');
        this.btnConfirmExit = document.getElementById('btnConfirmExit');

        // ── OS PiP Controller ─────────────────────────────────
        this.pip = new PipController();

        this._isCallActive = false;
        this._onExitConfirmed = null;

        // ── Timer State ──────────────────────────────────────
        this._startTime = null;
        this._timerInterval = null;
        this._wakeLock = null;

        // Smooth video transitions
        this.remoteVideoEl.style.transition = 'opacity 0.3s ease';
        this.remoteVideoEl.style.opacity = '0';
        this.localVideoEl.style.transition = 'opacity 0.3s ease, transform 0.3s ease';

        // Network awareness
        this._setupNetworkListeners();
        // Page lifecycle
        this._setupLifecycleListeners();
        // Interactive gestures
        this._setupGestureListeners();
    }

    _setupGestureListeners() {
        // Feature 1: Tap screen middle to toggle controls visibility
        if (this.uiLayer) {
            this.uiLayer.addEventListener('click', (e) => {
                // Ignore clicks on control buttons, status card, or video elements
                if (e.target.closest('#activeControls, .btn-join, .status-card, #localVideo, #remoteVideo')) return;
                document.body.classList.toggle('controls-hidden');
            });
        }

        // Feature 2: Tap self camera or remote video box (when in PiP mode) to swap local & remote video views
        const handleVideoClick = (e) => {
            e.stopPropagation();
            this.toggleVideoSwap();
        };

        if (this.localVideoEl) {
            this.localVideoEl.addEventListener('click', handleVideoClick);
        }
        if (this.remoteVideoEl) {
            this.remoteVideoEl.addEventListener('click', handleVideoClick);
        }
    }

    toggleVideoSwap() {
        const isSwapped = document.body.classList.toggle('video-swapped');
        this._updateAvatarVisibility();
        if (window.Android && typeof window.Android.onVideoSwapped === 'function') {
            window.Android.onVideoSwapped(isSwapped);
        }
        return isSwapped;
    }

    _updateAvatarVisibility() {
        const isSwapped = document.body.classList.contains('video-swapped');

        // If the host is full-screening their own video, hide avatar.
        if (this.isHost && isSwapped) {
            this.avatarContainer.style.opacity = '0';
            setTimeout(() => {
                if (document.body.classList.contains('video-swapped')) {
                    this.avatarContainer.style.display = 'none';
                }
            }, 300);
            return;
        } else if (this.isHost && !isSwapped) {
            return; // Host doesn't use the avatar container when remote is visible, handled natively by Android if remote is off
        }

        const isRemoteVideoVisible = this.remoteVideoEl &&
            this.remoteVideoEl.style.display !== 'none' &&
            this.remoteVideoEl.style.opacity !== '0';

        if (isSwapped || isRemoteVideoVisible) {
            this.avatarContainer.style.opacity = '0';
            setTimeout(() => {
                const currentlySwapped = document.body.classList.contains('video-swapped');
                const currentlyRemoteVisible = this.remoteVideoEl &&
                    this.remoteVideoEl.style.display !== 'none' &&
                    this.remoteVideoEl.style.opacity !== '0';
                if (currentlySwapped || currentlyRemoteVisible) {
                    this.avatarContainer.style.display = 'none';
                }
            }, 300);
            this.uiLayer.style.background = 'transparent';
        } else {
            this.avatarContainer.style.display = 'flex';
            setTimeout(() => {
                const currentlySwapped = document.body.classList.contains('video-swapped');
                const currentlyRemoteVisible = this.remoteVideoEl &&
                    this.remoteVideoEl.style.display !== 'none' &&
                    this.remoteVideoEl.style.opacity !== '0';
                if (!currentlySwapped && !currentlyRemoteVisible) {
                    this.avatarContainer.style.opacity = '1';
                }
            }, 50);
            this.uiLayer.style.background = '';
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Host Mode — hide web UI, transparent background
    // ─────────────────────────────────────────────────────────

    enterHostMode() {
        document.body.style.background = '#0f172a';
        document.body.classList.add('is-host');
        this.uiLayer.style.display = 'none';
        // Minimize button is a guest-only feature; CSS also hides it, but belt-and-suspenders
        if (this.minimizeContainer) this.minimizeContainer.style.display = 'none';
    }

    // ─────────────────────────────────────────────────────────
    //  Audio Visualizer (Guest)
    // ─────────────────────────────────────────────────────────

    updateAudioLevel(level) {
        if (!this.avatarPulse || this.isHost) return;

        if (this.avatarPulse.classList.contains('avatar-pulse')) {
            this.avatarPulse.classList.remove('avatar-pulse');
        }

        const scale = 1 + (level * 0.4);
        const glowOpacity = Math.max(0, level * 0.8);

        // Ensure container is circular so the box-shadow is a circle, not a square
        this.avatarPulse.style.borderRadius = '50%';
        this.avatarPulse.style.transform = `scale(${scale})`;
        this.avatarPulse.style.boxShadow = `0 0 0 ${40 * level}px rgba(124, 58, 237, ${glowOpacity})`;
        this.avatarPulse.style.transition = 'transform 0.1s ease-out, box-shadow 0.1s ease-out';
    }

    // ─────────────────────────────────────────────────────────
    //  Status Updates
    // ─────────────────────────────────────────────────────────

    setStatus(text) {
        if (this.statusText) this.statusText.textContent = text;
    }

    showInvalidLink() {
        const statusArea = document.querySelector('.status-area');
        const controlsArea = document.querySelector('.controls-area');

        if (statusArea) statusArea.style.display = 'none';
        if (controlsArea) controlsArea.style.display = 'none';
        if (this.avatarContainer) this.avatarContainer.style.display = 'none';

        const landingView = document.getElementById('landingView');
        if (landingView) {
            landingView.style.display = 'flex';
            landingView.classList.remove('hidden');
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Join Button States
    // ─────────────────────────────────────────────────────────

    setJoinLoading() {
        this.btnJoin.disabled = true;
        this.btnJoin.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Connecting...';
    }

    setJoinError(msg) {
        this.setStatus(msg);
        this.btnJoin.disabled = false;
        this.btnJoin.innerHTML = '<i class="fa-solid fa-phone-volume"></i> Try Again';
    }

    showActiveCallUI() {
        this._isCallActive = true;
        try { history.pushState({ inCall: true }, ''); } catch (_) { }
        if (window.Telegram?.WebApp) {
            try { window.Telegram.WebApp.enableClosingConfirmation(); } catch (_) { }
        }
        this.btnJoin.style.display = 'none';
        this.activeControls.style.display = 'flex';
        // Animate controls sliding up
        this.activeControls.style.transform = 'translateY(20px)';
        this.activeControls.style.opacity = '0';
        requestAnimationFrame(() => {
            this.activeControls.style.transition = 'transform 0.4s ease, opacity 0.4s ease';
            this.activeControls.style.transform = 'translateY(0)';
            this.activeControls.style.opacity = '1';
        });
        this.setStatus('Connected');
        if (this.statusText) {
            this.statusText.classList.remove('ended');
            this.statusText.classList.add('connected');
        }
        if (this.avatarPulse) this.avatarPulse.classList.remove('avatar-pulse');
    }

    // ─────────────────────────────────────────────────────────
    //  Stream Handling
    // ─────────────────────────────────────────────────────────

    attachLocalStream(stream, facingMode = 'user') {
        this.localVideoEl.srcObject = stream;
        this.localVideoEl.play().catch(e => console.error('[UI] Local video play failed', e));

        if (facingMode === 'user') {
            this.localVideoEl.classList.add('mirror');
        } else {
            this.localVideoEl.classList.remove('mirror');
        }
    }

    /** Attach the remote stream to the full-screen element. */
    attachRemoteStream(stream) {
        this.remoteVideoEl.srcObject = stream;
        this.remoteVideoEl.play().catch(() => { });
    }

    setRemoteFacingMode(mode) {
        if (mode === 'user') {
            this.remoteVideoEl.classList.add('mirror');
        } else {
            this.remoteVideoEl.classList.remove('mirror');
        }
    }

    /** Show/hide local PiP video. */
    setLocalVideoVisible(visible) {
        if (visible) {
            this.localVideoEl.style.display = 'block';
            this.localVideoEl.play().catch(() => { });
            requestAnimationFrame(() => {
                this.localVideoEl.style.opacity = '1';
                this.localVideoEl.style.transform = 'scale(1)';
            });
        } else {
            this.localVideoEl.style.opacity = '0';
            this.localVideoEl.style.transform = 'scale(0.85)';
            setTimeout(() => { this.localVideoEl.style.display = 'none'; }, 300);
        }
    }

    /** Show/hide remote video with smooth transitions. */
    setRemoteVideoVisible(visible, isHost) {
        if (isHost !== undefined) this.isHost = isHost;
        if (visible) {
            this.remoteVideoEl.style.display = 'block';
            this.remoteVideoEl.play().catch(() => { });
            setTimeout(() => {
                this.remoteVideoEl.style.opacity = '1';
                this._updateAvatarVisibility();
            }, 50);
        } else {
            this.remoteVideoEl.style.opacity = '0';
            setTimeout(() => {
                this.remoteVideoEl.style.display = 'none';
                this._updateAvatarVisibility();
            }, 300);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Control Button States
    // ─────────────────────────────────────────────────────────

    updateMuteButton(isMuted) {
        this.btnMute.className = isMuted ? 'btn-control btn-on' : 'btn-control btn-off';
        this.btnMute.innerHTML = isMuted
            ? '<i class="fa-solid fa-microphone-slash"></i>'
            : '<i class="fa-solid fa-microphone"></i>';
        if (this.lblMute) this.lblMute.textContent = isMuted ? 'Unmute' : 'Mute';
    }

    updateVideoButton(isVideoOn) {
        this.btnVideo.className = isVideoOn ? 'btn-control btn-on' : 'btn-control btn-off';
        this.btnVideo.innerHTML = isVideoOn
            ? '<i class="fa-solid fa-video"></i>'
            : '<i class="fa-solid fa-video-slash"></i>';
        if (this.lblVideo) this.lblVideo.textContent = isVideoOn ? 'Camera On' : 'Camera';

        if (this.flipContainer) {
            if (isVideoOn) {
                this.flipContainer.style.display = 'flex';
                this.flipContainer.style.animation = 'fadeUpIn 0.3s ease forwards';
            } else {
                this.flipContainer.style.display = 'none';
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Timer
    // ─────────────────────────────────────────────────────────

    startTimer() {
        if (this._startTime) return; // Prevent duplicates
        this._startTime = Date.now();
        this.durationText.style.display = 'block';
        this._timerInterval = setInterval(() => {
            const diff = Math.floor((Date.now() - this._startTime) / 1000);
            const mins = String(Math.floor(diff / 60)).padStart(2, '0');
            const secs = String(diff % 60).padStart(2, '0');
            this.durationText.textContent = `${mins}:${secs}`;
        }, 1000);
    }

    stopTimer() {
        clearInterval(this._timerInterval);
        this._timerInterval = null;
    }

    // ─────────────────────────────────────────────────────────
    //  Call Ended UI
    // ─────────────────────────────────────────────────────────

    showCallEnded(finalDuration) {
        this._isCallActive = false;
        this.hideExitModal();
        if (window.Telegram?.WebApp) {
            try { window.Telegram.WebApp.disableClosingConfirmation(); } catch (_) { }
        }
        if (this.pip) this.pip.close();
        this.stopTimer();
        this.remoteVideoEl.style.display = 'none';
        this.localVideoEl.style.display = 'none';
        this.activeControls.style.display = 'none';

        const statusArea = document.querySelector('.status-area');
        const controlsArea = document.querySelector('.controls-area');

        if (statusArea) statusArea.style.display = 'none';
        if (controlsArea) controlsArea.style.display = 'none';
        if (this.avatarContainer) this.avatarContainer.style.display = 'none';
        if (this.uiLayer) {
            this.uiLayer.style.display = 'flex';
            this.uiLayer.style.background = '';
        }

        const endedView = document.getElementById('endedView');
        if (endedView) {
            endedView.style.display = 'flex';
            endedView.classList.remove('hidden');
        }

        const durationDisplay = document.getElementById('finalDurationText');
        if (durationDisplay && finalDuration) {
            durationDisplay.textContent = finalDuration;
        }

        this.durationText.style.display = 'none';
        this._releaseWakeLock();
    }

    showCallFailed() {
        if (this.pip) this.pip.close();
        this.stopTimer();
        this.remoteVideoEl.style.display = 'none';
        this.localVideoEl.style.display = 'none';

        const statusArea = document.querySelector('.status-area');
        const controlsArea = document.querySelector('.controls-area');

        if (statusArea) statusArea.style.display = 'none';
        if (controlsArea) controlsArea.style.display = 'none';
        if (this.avatarContainer) this.avatarContainer.style.display = 'none';
        if (this.uiLayer) {
            this.uiLayer.style.display = 'flex';
            this.uiLayer.style.background = '';
        }

        const endedView = document.getElementById('endedView');
        if (endedView) {
            const title = endedView.querySelector('.landing-title');
            if (title) title.textContent = 'Call Failed';

            const subTitle = endedView.querySelector('.landing-subtitle');
            if (subTitle) subTitle.innerHTML = 'Host is unreachable or the link has expired.';

            endedView.style.display = 'flex';
            endedView.classList.remove('hidden');
        }
        this._releaseWakeLock();
    }

    // ─────────────────────────────────────────────────────────
    //  Toast Notifications
    // ─────────────────────────────────────────────────────────

    showToast(message, duration = 3000) {
        if (!this.toastContainer) return;
        const toast = document.createElement('div');
        toast.className = 'toast';
        toast.textContent = message;
        this.toastContainer.appendChild(toast);

        // Trigger entrance animation
        requestAnimationFrame(() => {
            toast.style.opacity = '1';
            toast.style.transform = 'translateY(0)';
        });

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(-10px)';
            setTimeout(() => toast.remove(), 300);
        }, duration);
    }

    // ─────────────────────────────────────────────────────────
    //  Screen Wake Lock (keep screen on during call)
    // ─────────────────────────────────────────────────────────

    async acquireWakeLock() {
        try {
            if ('wakeLock' in navigator) {
                this._wakeLock = await navigator.wakeLock.request('screen');
            }
        } catch (_) { /* Not supported or denied — non-critical */ }
    }

    _releaseWakeLock() {
        if (this._wakeLock) {
            this._wakeLock.release().catch(() => { });
            this._wakeLock = null;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Network Awareness
    // ─────────────────────────────────────────────────────────

    _setupNetworkListeners() {
        window.addEventListener('offline', () => {
            if (this.networkBanner) {
                this.networkBanner.textContent = 'No internet connection';
                this.networkBanner.style.display = 'block';
                requestAnimationFrame(() => (this.networkBanner.style.opacity = '1'));
            }
        });

        window.addEventListener('online', () => {
            if (this.networkBanner) {
                this.networkBanner.style.opacity = '0';
                setTimeout(() => (this.networkBanner.style.display = 'none'), 300);
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  Page Lifecycle (cleanup on close/navigate)
    // ─────────────────────────────────────────────────────────

    /** Register a cleanup callback for page unload. */
    onPageUnload(cleanupFn) {
        window.addEventListener('beforeunload', cleanupFn);
        window.addEventListener('pagehide', cleanupFn);
    }

    _setupLifecycleListeners() {
        this._setupExitWarningModal();
        this._wasBackgroundedWithoutPip = false;

        document.addEventListener('visibilitychange', async () => {
            if (document.visibilityState === 'hidden' && this._isCallActive && this.pip && !this.pip._isInPip) {
                this._wasBackgroundedWithoutPip = true;
            } else if (document.visibilityState === 'visible') {
                if (this._wakeLock) {
                    await this.acquireWakeLock();
                }
                if (this._wasBackgroundedWithoutPip && this._isCallActive && (!this.pip || !this.pip._isInPip)) {
                    this._wasBackgroundedWithoutPip = false;
                    this.showToast('⚠️ Enable Minimize to prevent call disconnect.', 4000);
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  Exit Warning Modal & Page Protection
    // ─────────────────────────────────────────────────────────

    _setupExitWarningModal() {
        // 0. Telegram Native App Close Confirmation (for Telegram Top 'X' Button)
        if (window.Telegram?.WebApp) {
            try {
                window.Telegram.WebApp.ready();
                window.Telegram.WebApp.enableClosingConfirmation();
            } catch (_) { }
        }

        // 1. Native Browser Close / Refresh Warning
        window.addEventListener('beforeunload', (e) => {
            if (this._isCallActive) {
                e.preventDefault();
                e.returnValue = 'Call in progress. Are you sure you want to leave?';
                return e.returnValue;
            }
        });

        // 2. Intercept Android Back Button / Mobile Swipe Back
        window.addEventListener('popstate', (e) => {
            if (this._isCallActive) {
                try { history.pushState({ inCall: true }, ''); } catch (_) { }
                this.showExitModal();
            }
        });

        // 3. Modal Controls
        if (this.btnCancelExit) {
            this.btnCancelExit.addEventListener('click', () => this.hideExitModal());
        }
        if (this.btnConfirmExit) {
            this.btnConfirmExit.addEventListener('click', () => {
                this._isCallActive = false;
                this.hideExitModal();
                if (this._onExitConfirmed) {
                    this._onExitConfirmed();
                } else {
                    window.location.reload();
                }
            });
        }
    }

    /** Register callback for when user explicitly confirms leaving call via modal */
    onExitConfirmed(fn) {
        this._onExitConfirmed = fn;
    }

    showExitModal() {
        if (this.exitModal) {
            this.exitModal.classList.add('active');
        }
    }

    hideExitModal() {
        if (this.exitModal) {
            this.exitModal.classList.remove('active');
        }
    }
}

// Singleton
export default new UIController();
