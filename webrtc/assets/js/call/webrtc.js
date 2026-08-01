/**
 * ─────────────────────────────────────────────────────────────
 *  Superiorchat Connect — WebRTC Manager
 * ─────────────────────────────────────────────────────────────
 *  Owns the entire PeerJS lifecycle: peer creation, media
 *  acquisition, call answering/placing, DataChannel messaging,
 *  ICE monitoring, and graceful cleanup.
 *
 *  This module is event-driven. The consumer (app.js) registers
 *  callbacks via the `on` object and never touches PeerJS directly.
 */

import {
    MEDIA_CONSTRAINTS,
    PEER_OPTIONS,
    MSG,
    BROKER_RECONNECT_DELAY,
    PEER_ID_RETRY_DELAY,
} from './config.js';

class WebRTCManager {
    constructor() {
        /** @type {Peer|null} */
        this.peer = null;
        /** @type {MediaStream|null} */
        this.localStream = null;
        /** @type {MediaCall|null} */
        this.currentCall = null;
        /** @type {DataConnection|null} */
        this.dataConn = null;

        this.isHost = false;
        this.isCallActive = false;
        this.isMuted = false;
        this.isVideoOn = false;
        this.remoteVideoOn = false;
        this.remoteVideoOn = false;
        this.hostId = null;
        this.currentFacingMode = 'user';
        this.isFlipping = false;

        // ── Event callbacks (set by app.js) ──────────────────────
        this.on = {
            ready:          (peerId)  => {},
            connected:      ()        => {},
            reconnecting:   ()        => {},
            remoteVideo:    (enabled) => {},
            remoteFacingMode: (mode)  => {},
            stream:         (stream)  => {},
            error:          (msg)     => {},
            ended:          ()        => {},
            iceState:       (state)   => {},
            audioLevel:     (level)   => {},
        };
        
        // ── Audio Visualizer ──────────────────────────────────────
        this.audioCtx = null;
        this.analyser = null;
        this.audioDataArray = null;
        this.isVisualizerRunning = false;
    }

    // ─────────────────────────────────────────────────────────
    //  Media
    // ─────────────────────────────────────────────────────────

    /** Acquire camera + mic. Both audio & video tracks remain enabled for SDP negotiation. */
    async acquireMedia() {
        try {
            const constraints = {
                audio: MEDIA_CONSTRAINTS.audio,
                video: { facingMode: this.currentFacingMode }
            };
            this.localStream = await navigator.mediaDevices.getUserMedia(constraints);
        } catch (e) {
            // Fallback for desktop cameras that do not support facingMode constraints
            console.warn('[WebRTC] facingMode constraint failed, falling back to default camera', e);
            this.localStream = await navigator.mediaDevices.getUserMedia(MEDIA_CONSTRAINTS);
        }
        return this.localStream;
    }

    // ─────────────────────────────────────────────────────────
    //  Peer Lifecycle
    // ─────────────────────────────────────────────────────────

    /**
     * Create a PeerJS instance and register all event handlers.
     * @param {string|null} id — fixed ID for host, null for guest (auto-ID)
     * @param {string|null} secret — cryptographic call password
     */
    setupPeer(id, secret) {
        this.hostId = id;
        this.expectedSecret = secret;
        this.peer = new Peer(id, PEER_OPTIONS);

        this.peer.on('open', (peerId) => {
            if (this.isCallActive) return; // Ignore broker reconnects mid-call
            this.on.ready(peerId);
        });

        this.peer.on('connection', (conn) => {
            // Guard: Validate Cryptographic Secret
            if (this.isHost && conn.metadata?.secret !== this.expectedSecret) {
                console.error('SECURITY: Rejecting DataChannel connection. Invalid secret.');
                conn.close();
                return;
            }

            // Guard: Prevent DataChannel hijacking from 3rd parties
            if (this.isCallActive && this.currentCall && this.currentCall.peer !== conn.peer) {
                console.warn('[WebRTC] Rejecting incoming DataChannel: call is already active with another peer.');
                conn.close();
                return;
            }

            this.dataConn = conn;
            this._setupDataChannel();
            
            conn.on('close', () => {
                if (this.dataConn === conn) this.dataConn = null;
            });
        });

        this.peer.on('call', (call) => {
            // Guard: Validate Cryptographic Secret
            if (this.isHost && call.metadata?.secret !== this.expectedSecret) {
                console.error('SECURITY: Rejecting incoming call. Invalid secret.');
                call.close();
                return;
            }

            // Guard: Prevent Call Hijacking if already in a call
            if (this.isCallActive) {
                console.warn('[WebRTC] Rejecting incoming call: another call is already active.');
                call.close();
                return;
            }

            if (!this.localStream) {
                console.error('[WebRTC] Incoming call but localStream is null');
                this.on.error('Media not ready');
                return;
            }
            
            this.currentCall = call;
            call.answer(this.localStream);
            this._handleCallStream(call);
            this.isCallActive = true;
            this.on.connected();
        });

        this.peer.on('error', (err) => this._handlePeerError(err));

        this.peer.on('disconnected', () => {
            if (!this.peer.destroyed && this.isCallActive) {
                setTimeout(() => {
                    if (this.peer && !this.peer.destroyed) this.peer.reconnect();
                }, BROKER_RECONNECT_DELAY);
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    //  Outbound Call (Guest → Host)
    // ─────────────────────────────────────────────────────────

    /** 
     * Silently ping the host to see if they are still online.
     * Returns a Promise that resolves to true if alive, false if dead/expired.
     */
    async checkHostActive(hostPeerId, secret, maxRetries = 3, onProgress = null) {
        for (let attempt = 1; attempt <= maxRetries; attempt++) {
            if (onProgress) onProgress(attempt, maxRetries);
            const isAlive = await new Promise((resolve) => {
                if (!this.peer || this.peer.destroyed) {
                    resolve(false);
                    return;
                }

                const conn = this.peer.connect(hostPeerId, { metadata: { secret: secret } });
                let isResolved = false;

                const finish = (result) => {
                    if (isResolved) return;
                    isResolved = true;
                    resolve(result);
                };
                
                // If connection opens, host is alive
                conn.on('open', () => {
                    conn.close();
                    finish(true);
                });

                // If error, host is not found yet
                const errorListener = (err) => {
                    if (err.type === 'peer-unavailable') {
                        this.peer.off('error', errorListener);
                        finish(false);
                    }
                };
                this.peer.on('error', errorListener);
                
                // Fallback timeout just in case network is weird
                setTimeout(() => {
                    this.peer.off('error', errorListener);
                    finish(false);
                }, 4000);
            });

            if (isAlive) {
                return true;
            }

            // If this isn't the last attempt, wait before retrying
            if (attempt < maxRetries) {
                await new Promise(r => setTimeout(r, 1500));
            }
        }
        return false;
    }

    /** Place a call to the host and open a DataChannel. */
    placeCall(hostPeerId, secret) {
        if (!this.localStream || !this.peer) return;

        this.currentCall = this.peer.call(hostPeerId, this.localStream, { metadata: { secret: secret } });
        this._handleCallStream(this.currentCall);

        this.dataConn = this.peer.connect(hostPeerId, { metadata: { secret: secret } });
        this._setupDataChannel();

        this.isCallActive = true;
    }

    // ─────────────────────────────────────────────────────────
    //  Toggle Controls & Camera Flip
    // ─────────────────────────────────────────────────────────

    toggleAudio() {
        if (!this.localStream) return;
        this.isMuted = !this.isMuted;
        this.localStream.getAudioTracks().forEach(t => (t.enabled = !this.isMuted));
        return this.isMuted;
    }

    toggleVideo() {
        if (!this.localStream) return;
        this.isVideoOn = !this.isVideoOn;
        // Notify remote peer via DataChannel to toggle UI video layer visibility
        this._sendData({ type: MSG.VIDEO_STATE, enabled: this.isVideoOn });
        return this.isVideoOn;
    }

    async flipCamera() {
        if (!this.localStream || this.isFlipping) return;
        this.isFlipping = true;
        
        // 1. Toggle facing mode
        const previousFacingMode = this.currentFacingMode;
        this.currentFacingMode = this.currentFacingMode === 'user' ? 'environment' : 'user';
        
        try {
            // 2. Stop old video track to release hardware lock (CRITICAL on Android)
            const oldVideoTrack = this.localStream.getVideoTracks()[0];
            if (oldVideoTrack) {
                oldVideoTrack.stop();
                this.localStream.removeTrack(oldVideoTrack);
            }

            // 3. Request new video stream strictly for the new camera
            const newStream = await navigator.mediaDevices.getUserMedia({ 
                video: { facingMode: this.currentFacingMode },
                audio: false
            });
            const newVideoTrack = newStream.getVideoTracks()[0];

            // 4. Attach new track to local stream
            this.localStream.addTrack(newVideoTrack);

            // 5. Instantly replace the track in the active WebRTC connection (Zero-Lag)
            if (this.currentCall && this.currentCall.peerConnection) {
                const sender = this.currentCall.peerConnection.getSenders().find(s => s.track && s.track.kind === 'video');
                if (sender) {
                    await sender.replaceTrack(newVideoTrack);
                }
            }

            // 6. Notify UI to reattach the updated stream to the video element
            if (this.on.localStreamFlipped) {
                this.on.localStreamFlipped(this.localStream, this.currentFacingMode);
            }
            
            // 7. Sync new facing mode to remote peer
            this._sendData({ type: MSG.FACING_MODE, facingMode: this.currentFacingMode });
        } catch (error) {
            console.error('[WebRTC] Failed to flip camera:', error);
            // Revert state if failed and try to restart old camera
            this.currentFacingMode = previousFacingMode;
            
            try {
                const recoveryStream = await navigator.mediaDevices.getUserMedia({
                    video: { facingMode: this.currentFacingMode },
                    audio: false
                });
                const recoveryTrack = recoveryStream.getVideoTracks()[0];
                this.localStream.addTrack(recoveryTrack);
                
                if (this.currentCall && this.currentCall.peerConnection) {
                    const sender = this.currentCall.peerConnection.getSenders().find(s => s.track && s.track.kind === 'video' || s.track === null);
                    if (sender) await sender.replaceTrack(recoveryTrack);
                }
                
                if (this.on.localStreamFlipped) {
                    this.on.localStreamFlipped(this.localStream, this.currentFacingMode);
                }
            } catch (recoveryErr) {
                console.error('[WebRTC] Failed to recover original camera:', recoveryErr);
            }
        } finally {
            this.isFlipping = false;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  End / Cleanup
    // ─────────────────────────────────────────────────────────

    endCall() {
        if (this._isEnding) return;
        this._isEnding = true;
        
        this.isCallActive = false;
        if (this.currentCall) this.currentCall.close();
        if (this.dataConn) this.dataConn.close();
        if (this.localStream) {
            this.localStream.getTracks().forEach(t => t.stop());
            this.localStream = null;
        }
        if (this.peer && !this.peer.destroyed) this.peer.destroy();
        this._stopAudioVisualizer();
        this.on.ended();
        
        setTimeout(() => { this._isEnding = false; }, 1000);
    }

    // ─────────────────────────────────────────────────────────
    //  Private — DataChannel
    // ─────────────────────────────────────────────────────────

    _setupDataChannel() {
        if (!this.dataConn) return;

        this.dataConn.on('open', () => {
            // Immediately sync video state on channel open
            this._sendData({ type: MSG.VIDEO_STATE, enabled: this.isVideoOn });
            this._sendData({ type: MSG.FACING_MODE, facingMode: this.currentFacingMode });
        });

        this.dataConn.on('data', (raw) => {
            try {
                const msg = JSON.parse(raw);
                if (msg.type === MSG.VIDEO_STATE) {
                    this.remoteVideoOn = msg.enabled;
                    this.on.remoteVideo(msg.enabled);
                } else if (msg.type === MSG.FACING_MODE) {
                    this.on.remoteFacingMode(msg.facingMode);
                }
            } catch (_) { /* ignore malformed */ }
        });
    }

    _sendData(obj) {
        if (this.dataConn && this.dataConn.open) {
            this.dataConn.send(JSON.stringify(obj));
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Private — Call Stream + ICE Monitoring
    // ─────────────────────────────────────────────────────────

    _handleCallStream(call) {
        if (!call) return;
        call.on('stream', (remoteStream) => {
            this.on.stream(remoteStream);
            this._startAudioVisualizer(remoteStream);
            this._monitorICE(call);
        });

        call.on('close', () => {
            console.log('[WebRTC] Call closed by remote peer');
            this.endCall();
        });

        call.on('error', (err) => {
            console.error('[WebRTC] Call error:', err);
        });
    }

    _monitorICE(call) {
        if (!call?.peerConnection) return;
        const pc = call.peerConnection;
        let iceDisconnectTimeout = null;

        pc.oniceconnectionstatechange = () => {
            const state = pc.iceConnectionState;
            this.on.iceState(state);

            if (state === 'disconnected') {
                this.on.reconnecting();
                // Start a watchdog timer. If ICE stays disconnected for 15s, kill it.
                iceDisconnectTimeout = setTimeout(() => {
                    console.error('[WebRTC] ICE disconnected timeout reached. Forcing call end.');
                    this.endCall();
                }, 15000);
            } else if (state === 'connected' || state === 'completed') {
                if (iceDisconnectTimeout) {
                    clearTimeout(iceDisconnectTimeout);
                    iceDisconnectTimeout = null;
                }
                if (this.isCallActive) this.on.connected();
            } else if (state === 'failed') {
                if (iceDisconnectTimeout) {
                    clearTimeout(iceDisconnectTimeout);
                    iceDisconnectTimeout = null;
                }
                console.error('[WebRTC] ICE connection failed.');
                this.endCall();
            }
        };
    }

    // ─────────────────────────────────────────────────────────
    //  Private — Error Handling
    // ─────────────────────────────────────────────────────────

    _handlePeerError(err) {
        console.error('[WebRTC] PeerJS error:', err.type, err.message);

        if (err.type === 'unavailable-id') {
            // Stale broker entry — retry
            if (this.isHost && !this.isCallActive) {
                this.peer.destroy();
                setTimeout(() => this.setupPeer(this.hostId, this.expectedSecret), PEER_ID_RETRY_DELAY);
            }
            return;
        }

        if (err.type === 'peer-unavailable' && !this.isCallActive) {
            this.on.error('Host not ready. Try again in a moment.');
            return;
        }

        // Fatal
        this.on.error('Connection error');
    }
    
    // ─────────────────────────────────────────────────────────
    //  Private — Audio Visualizer
    // ─────────────────────────────────────────────────────────

    _startAudioVisualizer(stream) {
        if (!window.AudioContext && !window.webkitAudioContext) return;
        if (!this.audioCtx) {
            this.audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        }
        
        if (this.audioCtx.state === 'suspended') {
            this.audioCtx.resume().catch(() => {});
        }
        
        if (stream.getAudioTracks().length === 0) return;
        
        this.analyser = this.audioCtx.createAnalyser();
        this.analyser.fftSize = 256;
        this.analyser.smoothingTimeConstant = 0.8;
        
        const source = this.audioCtx.createMediaStreamSource(stream);
        source.connect(this.analyser);
        
        const bufferLength = this.analyser.frequencyBinCount;
        this.audioDataArray = new Uint8Array(bufferLength);
        
        this.isVisualizerRunning = true;
        
        const renderFrame = () => {
            if (!this.isVisualizerRunning) return;
            requestAnimationFrame(renderFrame);
            
            this.analyser.getByteFrequencyData(this.audioDataArray);
            
            let sum = 0;
            for (let i = 0; i < bufferLength; i++) {
                sum += this.audioDataArray[i];
            }
            let average = sum / bufferLength;
            // Amplify sensitivity: Human speech average is usually low (10-30). 
            // Divide by 50 instead of 128 so normal speech hits closer to 0.5 - 1.0
            let level = Math.min(average / 50, 1.0);
            
            // Pass level directly out
            this.on.audioLevel(level);
        };
        
        renderFrame();
    }

    _stopAudioVisualizer() {
        this.isVisualizerRunning = false;
        if (this.audioCtx && this.audioCtx.state !== 'closed') {
            this.audioCtx.close().catch(() => {});
            this.audioCtx = null;
        }
    }
}

// Singleton
export default new WebRTCManager();
