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

class UIController {
    constructor() {
        // ── DOM References ────────────────────────────────────
        this.statusText     = document.getElementById('statusText');
        this.durationText   = document.getElementById('durationText');
        this.avatarContainer= document.getElementById('avatarContainer');
        this.avatarPulse    = document.querySelector('.avatar-pulse');
        this.btnJoin        = document.getElementById('btnJoin');
        this.activeControls = document.getElementById('activeControls');
        this.remoteVideoEl  = document.getElementById('remoteVideo');
        this.localVideoEl   = document.getElementById('localVideo');
        this.uiLayer        = document.getElementById('ui-layer');
        this.btnMute        = document.getElementById('btnMute');
        this.btnVideo       = document.getElementById('btnVideo');
        this.btnFlipCamera  = document.getElementById('btnFlipCamera');
        this.flipContainer  = document.getElementById('flipContainer');
        this.btnEnd         = document.getElementById('btnEnd');
        this.lblMute        = document.getElementById('lblMute');
        this.lblVideo       = document.getElementById('lblVideo');
        this.toastContainer = document.getElementById('toastContainer');
        this.networkBanner  = document.getElementById('networkBanner');

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
        this.remoteVideoEl.play().catch(() => {});
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
            this.localVideoEl.play().catch(() => {});
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
            this.remoteVideoEl.play().catch(() => {});
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
            this._wakeLock.release().catch(() => {});
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
        // Re-acquire wake lock when tab becomes visible again
        document.addEventListener('visibilitychange', async () => {
            if (document.visibilityState === 'visible' && this._wakeLock) {
                await this.acquireWakeLock();
            }
        });
    }
}

// Singleton
export default new UIController();
