package xyz.mufanc.popit

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log
import android.widget.Toast
import kotlin.math.roundToInt

class PopitVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle): VoiceInteractionSession =
        Session(this).also { activeSession = it }

    companion object {
        @Volatile
        private var activeSession: Session? = null

        fun publishCaptured() = activeSession?.publishCaptured() == true
    }

    private class Session(context: Context) : VoiceInteractionSession(context) {
        private var captured = false
        private var receivedStates = 0
        private var pendingComponent: ComponentName? = null
        private var pendingIntent: Intent? = null

        override fun onCreate() {
            super.onCreate()
            setUiEnabled(false)
        }

        override fun onHandleAssist(state: AssistState) {
            receivedStates++
            val component = state.assistStructure?.activityComponent
            val intent = state.assistContent?.intent
            Log.i(
                Constants.TAG,
                "Assist state: focused=${state.isFocused}, component=$component, " +
                    "hasIntent=${intent != null}",
            )

            if (!captured && component != null && intent != null &&
                context.packageName != component.packageName
            ) {
                captured = true
                if (!isExported(component)) {
                    Log.w(Constants.TAG, "Captured activity is not exported: $component")
                    Toast.makeText(context, R.string.activity_not_exported, Toast.LENGTH_SHORT)
                        .show()
                    PopitAssistActivity.onCaptureFailed()
                    finish()
                    return
                }
                pendingComponent = component
                pendingIntent = intent
                BubbleDismissReceiver.dismiss(context, "bubble:${component.packageName}")
                if (!PopitAssistActivity.onCaptured()) {
                    Log.e(Constants.TAG, "Bubble bridge activity is no longer active")
                    Toast.makeText(context, R.string.bubble_failed, Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else if (!captured && receivedStates == state.count) {
                Toast.makeText(context, R.string.capture_failed, Toast.LENGTH_SHORT).show()
                PopitAssistActivity.onCaptureFailed()
                finish()
            }
        }

        fun publishCaptured(): Boolean {
            val component = pendingComponent ?: return false
            val intent = pendingIntent ?: return false
            pendingComponent = null
            pendingIntent = null
            try {
                postBubble(component, intent)
            } catch (error: RuntimeException) {
                Log.e(Constants.TAG, "Unable to post captured activity as a bubble", error)
                Toast.makeText(context, R.string.bubble_failed, Toast.LENGTH_SHORT).show()
            } finally {
                finish()
            }
            return true
        }

        private fun isExported(component: ComponentName): Boolean = try {
            context.packageManager.getActivityInfo(
                component,
                PackageManager.ComponentInfoFlags.of(0),
            ).exported
        } catch (error: PackageManager.NameNotFoundException) {
            Log.w(Constants.TAG, "Captured activity could not be resolved: $component", error)
            false
        }

        private fun postBubble(component: ComponentName, capturedIntent: Intent) {
            val packageName = component.packageName
            val packages = context.packageManager
            val application = try {
                packages.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0),
                )
            } catch (error: PackageManager.NameNotFoundException) {
                throw IllegalArgumentException("Target application is missing: $packageName", error)
            }
            val label = application.loadLabel(packages).toString()
            val icon = createIcon(application.loadIcon(packages))
            val app = Person.Builder()
                .setName(label)
                .setKey(packageName)
                .setImportant(true)
                .build()
            val shortcutId = "bubble:$packageName"
            val notifications = context.getSystemService(NotificationManager::class.java)
            val shortcuts = context.getSystemService(ShortcutManager::class.java)

            shortcuts.pushDynamicShortcut(
                ShortcutInfo.Builder(context, shortcutId)
                    .setShortLabel(label)
                    .setIcon(icon)
                    .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW))
                    .setPerson(app)
                    .setLongLived(true)
                    .build(),
            )

            val bubbleIntent = PendingIntent.getActivity(
                context,
                shortcutId.hashCode(),
                Intent(capturedIntent).setComponent(component),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            val deleteIntent = PendingIntent.getBroadcast(
                context,
                shortcutId.hashCode(),
                Intent(context, BubbleDismissReceiver::class.java)
                    .putExtra(BubbleDismissReceiver.EXTRA_SHORTCUT_ID, shortcutId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val bubble = Notification.BubbleMetadata.Builder(bubbleIntent, icon)
                .setDeleteIntent(deleteIntent)
                .setDesiredHeight(Int.MAX_VALUE)
                .setAutoExpandBubble(true)
                .build()

            notifications.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { setAllowBubbles(true) },
            )

            val user = Person.Builder().setName(context.getString(R.string.app_name)).build()
            val notification = Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_popit)
                .setContentTitle(label)
                .setContentText(context.getString(R.string.bubble_ready))
                .setContentIntent(bubbleIntent)
                .setDeleteIntent(deleteIntent)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setShortcutId(shortcutId)
                .setStyle(
                    Notification.MessagingStyle(user).addMessage(
                        context.getString(R.string.bubble_ready),
                        System.currentTimeMillis(),
                        app,
                    ),
                )
                .setBubbleMetadata(bubble)
                .setOnlyAlertOnce(true)
                .build()
            notifications.notify(shortcutId, 0, notification)
            Log.i(
                Constants.TAG,
                "Bubble notification posted: component=$component, shortcutId=$shortcutId, " +
                    "bubblePreference=${notifications.bubblePreference}, " +
                    "bubblesEnabled=${notifications.areBubblesEnabled()}",
            )
        }

        private fun createIcon(drawable: Drawable): Icon {
            val size = (64 * context.resources.displayMetrics.density).roundToInt()
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(bitmap))
            return Icon.createWithBitmap(bitmap)
        }

        override fun onDestroy() {
            if (activeSession === this) activeSession = null
            super.onDestroy()
        }

        companion object {
            private const val CHANNEL_ID = "captured_activities"
        }
    }
}
