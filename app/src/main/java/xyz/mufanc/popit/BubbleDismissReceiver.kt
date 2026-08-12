package xyz.mufanc.popit

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager

class BubbleDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val shortcutId = intent.getStringExtra(EXTRA_SHORTCUT_ID)
        if (shortcutId?.startsWith("bubble:") != true) return
        dismiss(context, shortcutId)
    }

    companion object {
        const val EXTRA_SHORTCUT_ID = "shortcut_id"

        fun dismiss(context: Context, shortcutId: String) {
            context.getSystemService(NotificationManager::class.java).cancel(shortcutId, 0)
            context.getSystemService(ShortcutManager::class.java)
                .removeLongLivedShortcuts(listOf(shortcutId))
        }
    }
}
