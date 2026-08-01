/**
 * ─────────────────────────────────────────────────────────────
 *  Superiorchat Connect — Configuration
 * ─────────────────────────────────────────────────────────────
 *  Central configuration for ICE servers, media constraints,
 *  and application constants. Edit this file to add TURN
 *  servers or adjust audio/video quality.
 */

export const ICE_SERVERS = [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
    { urls: 'stun:stun2.l.google.com:19302' },
    { urls: 'stun:stun3.l.google.com:19302' },
    // ── Add TURN servers below for VPN/firewall support ──
    // {
    //     urls: 'turn:your-server.com:443?transport=tcp',
    //     username: 'user',
    //     credential: 'pass'
    // }
];

export const MEDIA_CONSTRAINTS = {
    video: true,
    audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
        sampleRate: 48000,
    },
};

export const PEER_OPTIONS = {
    debug: 1,
    config: { iceServers: ICE_SERVERS },
};

/** DataChannel message types */
export const MSG = {
    VIDEO_STATE: 'video_state',
    FACING_MODE: 'facing_mode'
};

/** Reconnection delay when PeerJS broker disconnects (ms) */
export const BROKER_RECONNECT_DELAY = 2000;

/** Delay before retrying a conflicting Peer ID (ms) */
export const PEER_ID_RETRY_DELAY = 1000;
