package xyz.mufanc.popit

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.app.StatusBarManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val launcher by lazy { ComponentName(this, "$packageName.Launcher") }
    private lateinit var roleManager: RoleManager
    private lateinit var notifications: NotificationManager
    private lateinit var assistantStatus: TextView
    private lateinit var assistantAction: Button
    private lateinit var notificationStatus: TextView
    private lateinit var notificationAction: Button
    private lateinit var bubbleStatus: TextView
    private lateinit var bubbleAction: Button
    private lateinit var setupStatus: TextView

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_main)

        roleManager = getSystemService(RoleManager::class.java)
        notifications = getSystemService(NotificationManager::class.java)
        assistantStatus = findViewById(R.id.assistant_status)
        assistantAction = findViewById<Button>(R.id.assistant_action).apply {
            setOnClickListener { openAssistantSettings() }
        }
        notificationStatus = findViewById(R.id.notification_status)
        notificationAction = findViewById<Button>(R.id.notification_action).apply {
            setOnClickListener { configureNotifications() }
        }
        bubbleStatus = findViewById(R.id.bubble_status)
        bubbleAction = findViewById<Button>(R.id.bubble_action).apply {
            setOnClickListener { openBubbleSettings() }
        }
        setupStatus = findViewById(R.id.setup_status)
        findViewById<Button>(R.id.tile_action).setOnClickListener { requestTile() }
        findViewById<Switch>(R.id.hide_icon).apply {
            isChecked = !launcherIconVisible()
            isEnabled = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CALLING)
            setOnCheckedChangeListener { _, hidden -> setLauncherIconVisible(!hidden) }
        }
    }

    override fun onResume() {
        super.onResume()
        updateState()
    }

    private fun openAssistantSettings() {
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
        if (intent.resolveActivity(packageManager) == null) {
            intent.action = Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS
        }
        startActivity(intent)
    }

    private fun configureNotifications() {
        if (notificationsAllowed()) {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS,
            )
        }
    }

    private fun openBubbleSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        )
    }

    private fun requestTile() {
        getSystemService(StatusBarManager::class.java).requestAddTileService(
            ComponentName(this, PopitTileService::class.java),
            getString(R.string.app_name),
            Icon.createWithResource(this, R.drawable.ic_popit),
            mainExecutor,
        ) { result ->
            if (result != StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED &&
                result != StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
            ) {
                Toast.makeText(this, R.string.tile_add_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun notificationsAllowed() =
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun bubblesAllowed() =
        notifications.bubblePreference != NotificationManager.BUBBLE_PREFERENCE_NONE

    private fun launcherIconVisible(): Boolean {
        val state = packageManager.getComponentEnabledSetting(launcher)
        return state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ||
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    private fun setLauncherIconVisible(visible: Boolean) {
        packageManager.setComponentEnabledSetting(
            launcher,
            if (visible) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    private fun updateState() {
        val roleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)
        val assistantReady = roleAvailable && roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)
        val notificationsReady = notificationsAllowed()
        val bubblesReady = bubblesAllowed()

        assistantStatus.setText(
            when {
                !roleAvailable -> R.string.status_unavailable
                assistantReady -> R.string.status_enabled
                else -> R.string.status_disabled
            },
        )
        assistantAction.isEnabled = roleAvailable
        assistantAction.setText(if (assistantReady) R.string.action_manage else R.string.action_choose)
        notificationStatus.setText(
            if (notificationsReady) R.string.status_enabled else R.string.status_disabled,
        )
        notificationAction.setText(
            if (notificationsReady) R.string.action_manage else R.string.action_allow,
        )
        bubbleStatus.setText(if (bubblesReady) R.string.status_enabled else R.string.status_disabled)
        bubbleAction.setText(if (bubblesReady) R.string.action_manage else R.string.action_enable)
        setupStatus.setText(
            if (assistantReady && notificationsReady && bubblesReady) {
                R.string.setup_ready
            } else {
                R.string.setup_incomplete
            },
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) updateState()
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 1
    }
}
