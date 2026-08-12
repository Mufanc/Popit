package xyz.mufanc.popit

import android.content.Intent
import android.os.RemoteException
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import android.util.Log

class PopitRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent, listener: Callback) {
        try {
            listener.error(SpeechRecognizer.ERROR_CLIENT)
        } catch (error: RemoteException) {
            Log.w(Constants.TAG, "Recognition callback is gone", error)
        }
    }

    override fun onCancel(listener: Callback) = Unit

    override fun onStopListening(listener: Callback) = Unit
}
