package xyz.mufanc.popit

import android.os.Build
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession

class PopitVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        instance = this
    }

    override fun onShutdown() {
        instance = null
        super.onShutdown()
    }

    companion object {
        @Volatile
        private var instance: PopitVoiceInteractionService? = null

        fun captureTopActivity(): Boolean {
            val service = instance ?: return false
            var flags = VoiceInteractionSession.SHOW_WITH_ASSIST
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                flags = flags or
                    VoiceInteractionSession.SHOW_WITH_ASSIST_STRUCTURE_SCREEN_CONTENT
            }
            service.showSession(null, flags)
            return true
        }
    }
}
