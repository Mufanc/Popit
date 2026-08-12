package xyz.mufanc.popit

import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class PopitTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = getString(R.string.app_name)
            subtitle = getString(R.string.tile_subtitle)
            state = if (isReady()) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        Log.i(Constants.TAG, "Tile clicked")
        unlockAndRun {
            val ready = isReady()
            Log.i(Constants.TAG, "Tile unlocked: ready=$ready")
            if (ready) capture() else openSetup()
        }
    }

    private fun isReady() =
        getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT)

    private fun openSetup() {
        startAndCollapse(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            0,
        )
    }

    private fun capture() {
        startAndCollapse(
            Intent(this, PopitAssistActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_HISTORY,
            ),
            1,
        )
    }

    private fun startAndCollapse(intent: Intent, requestCode: Int) {
        startActivityAndCollapse(
            PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }
}
