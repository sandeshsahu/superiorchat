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
    var clickCount: Int = 0
    var lastClickTime: Long = 0L
}

class CamouflageTileService : TileService() {

    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { resetTileState() }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(if (TileUnlockState.clickCount > 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE)
    }

    override fun onClick() {
        super.onClick()
        
        val prefs = AppGraph.prefs
        if (!prefs.isTileAccessEnabled) {
            updateTileState(Tile.STATE_INACTIVE)
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - TileUnlockState.lastClickTime > 1500) {
            TileUnlockState.clickCount = 0
        }
        
        TileUnlockState.lastClickTime = currentTime
        TileUnlockState.clickCount++

        handler.removeCallbacks(timeoutRunnable)

        when (TileUnlockState.clickCount) {
            1 -> {
                updateTileState(Tile.STATE_ACTIVE)
                handler.postDelayed(timeoutRunnable, 5000)
            }
            2 -> {
                updateTileState(Tile.STATE_INACTIVE)
                handler.postDelayed(timeoutRunnable, 5000)
            }
            3 -> {
                updateTileState(Tile.STATE_ACTIVE)
                TileUnlockState.lastUnlockTimestamp = System.currentTimeMillis()
                AppLog.log(com.mobile.superiorchat.utils.LogCategory.SYSTEM, "3-click sequence complete. Unlock flag set.", com.mobile.superiorchat.utils.LogLevel.DEBUG)
                handler.postDelayed(timeoutRunnable, 5000)
                TileUnlockState.clickCount = 0
            }
        }
    }

    private fun resetTileState() {
        TileUnlockState.clickCount = 0
        TileUnlockState.lastUnlockTimestamp = 0L
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
}
