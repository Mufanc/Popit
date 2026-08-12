package xyz.mufanc.popit

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class PopitAssistActivity : Activity() {
    private var captureRequested = false
    private var homeRequested = false
    private var published = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
    }

    override fun onResume() {
        super.onResume()
        active = this
        if (captureRequested) return
        captureRequested = true
        if (!PopitVoiceInteractionService.captureTopActivity()) {
            Toast.makeText(this, R.string.assistant_not_ready, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun openHome() {
        if (homeRequested) return
        homeRequested = true
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION,
                    ),
            )
        } catch (error: RuntimeException) {
            Log.e(Constants.TAG, "Unable to open Home before posting bubble", error)
            publishBubble()
        }
    }

    override fun onStop() {
        super.onStop()
        if (homeRequested) publishBubble()
    }

    private fun publishBubble() {
        if (published) return
        published = true
        if (!PopitVoiceInteractionSessionService.publishCaptured()) {
            Toast.makeText(this, R.string.capture_failed, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onDestroy() {
        if (active === this) active = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var active: PopitAssistActivity? = null

        fun onCaptured(): Boolean {
            val activity = active ?: return false
            activity.runOnUiThread(activity::openHome)
            return true
        }

        fun onCaptureFailed() {
            active?.let { activity -> activity.runOnUiThread(activity::finish) }
        }
    }
}
