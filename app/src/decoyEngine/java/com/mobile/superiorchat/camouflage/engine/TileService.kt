package com.mobile.superiorchat.camouflage.engine

import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mobile.superiorchat.core.AppGraph
import com.mobile.superiorchat.utils.AppLog

import android.graphics.drawable.Icon
import com.mobile.superiorchat.R

object TileUnlockState {
    var lastUnlockTimestamp: Long = 0L
    var tapCount: Int = 0
    var lastTapTime: Long = 0L

    fun registerClick(isTileActive: Boolean) {
        val currentTime = System.currentTimeMillis()
        
        // If they paused for more than 3 seconds, reset the session
        if (currentTime - lastTapTime > 3000) {
            tapCount = 0
            lastUnlockTimestamp = 0L
        }
        
        lastTapTime = currentTime
        tapCount++

        if (tapCount == 3 && isTileActive) {
            // Perfect 3 taps ending on ACTIVE
            lastUnlockTimestamp = currentTime
            AppLog.log(com.mobile.superiorchat.utils.LogCategory.SYSTEM, "Valid 3-tap combo locked in.", com.mobile.superiorchat.utils.LogLevel.DEBUG)
        } else {
            // If they tap < 3 or > 3, ensure unlock flag is wiped
            lastUnlockTimestamp = 0L
        }
    }

    fun resetSession() {
        tapCount = 0
        lastUnlockTimestamp = 0L
        lastTapTime = 0L
    }
}

class CamouflageTileService : TileService() {

    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { resetTileState() }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        
        // If the tile has been idle for more than 5 seconds, visually force it OFF.
        // This guarantees the tile auto-disables after opening the chat app.
        if (System.currentTimeMillis() - TileUnlockState.lastTapTime > 5000) {
            updateTileState(Tile.STATE_INACTIVE)
        } else if (tile.state == Tile.STATE_UNAVAILABLE) {
            updateTileState(Tile.STATE_INACTIVE)
        }
    }

    override fun onClick() {
        super.onClick()
        
        val prefs = AppGraph.prefs
        if (!prefs.isTileAccessEnabled) {
            updateTileState(Tile.STATE_INACTIVE)
            return
        }

        val tile = qsTile ?: return
        
        // Always toggle the visual state to prevent UI freezing
        val newState = if (tile.state == Tile.STATE_ACTIVE) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        updateTileState(newState)

        // Register the click in the Pause-Reset session tracker
        TileUnlockState.registerClick(newState == Tile.STATE_ACTIVE)

        handler.removeCallbacks(timeoutRunnable)

        // Wipe visual state after 5s of inactivity
        handler.postDelayed(timeoutRunnable, 5000)
    }

    private fun resetTileState() {
        TileUnlockState.resetSession()
        updateTileState(Tile.STATE_INACTIVE)
    }

    private fun updateTileState(state: Int) {
        val tile = qsTile
        if (tile != null) {
            tile.state = state
            tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_tile)
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
