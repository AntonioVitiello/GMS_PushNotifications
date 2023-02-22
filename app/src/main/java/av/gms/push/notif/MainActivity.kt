package av.gms.push.notif

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.security.ProviderInstaller
import com.google.android.gms.tasks.Tasks
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Firebase console:
 * https://console.firebase.google.com
 * project name: GMS Push Notifications
 */
class MainActivity : AppCompatActivity() {
    private var currentPushToken: String? = null
    private var fid: String? = null

    companion object {
        const val TAG = "AAA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        securityUpdate(this)
        handleIntent(intent)
        initPushNotifications()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent) {
        intent.extras?.let { bundle ->
            val myKey = "antonio"
            val myValue = bundle.getString(myKey)
            Log.i(TAG, "PushNotification key-value data: [$myKey] -> [$myValue]")

            bundle.keySet()?.forEach { key ->
                val value = bundle.getString(key)
                Log.i(TAG, "PushNotification key-value data: [$key] -> [$value]")
            }
        }
    }

    fun initPushNotifications() {
        try {
            getFid { fid: String ->
                this.fid = fid
                Log.d(TAG, "GMS FID=[$fid]")
            }
            getDeviceTokenAsync { token: String ->
                currentPushToken = token
                Log.d(TAG, "GMS PushToken:\n[$token]")
            }
        } catch (e: Exception) {
            Log.e(TAG, "GMS getToken failed", e)
        }
    }

    /**
     * Must not be called on the main application thread!
     */
    fun getDeviceToken(): String {
        return Tasks.await(FirebaseMessaging.getInstance().token).also { token: String ->
            Log.d(TAG, "GMS getDeviceToken:$token")
        }
    }

    fun getDeviceTokenAsync(task: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token: String ->
            task(token)
            Log.d(TAG, "GMS getDeviceTokenAsync:$token")
        }
    }

    fun deleteToken() {
        try {
            FirebaseMessaging.getInstance().deleteToken()
            FirebaseInstallations.getInstance().delete()
            FirebaseInstallations.getInstance().getId()
            currentPushToken = null
            Log.i(TAG, "GMS PushToken deleted successfully")
        } catch (exc: Exception) {
            Log.e(TAG, "GMS Delete PushToken failed", exc)
        }
    }

    /**
     * return Firebase Installation Id: FID
     */
    fun getFid(task: (String) -> Unit) {
        try {
            FirebaseInstallations.getInstance().id.addOnSuccessListener { fid: String ->
                task(fid)
                Log.d(TAG, "GMS FID=$fid")
            }
        } catch (exc: Exception) {
            Log.e(TAG, "GMS failed getting FID", exc)
        }
    }

    override fun onDestroy() {
//        deleteToken()
        super.onDestroy()
    }

    fun openNotificationsSettings(context: Context, channelId: String? = null) {
        val pushSettingsIntent = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O /*26*/ -> Intent().apply {
                action = when (channelId) {
                    null -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    else -> Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                }
                channelId?.let { putExtra(Settings.EXTRA_CHANNEL_ID, it) }
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P /*28*/) {
                    flags += Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            else -> Intent().apply {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("app_package", context.packageName)
                putExtra("app_uid", context.applicationInfo.uid)
            }
        }
        pushSettingsIntent.let(context::startActivity)
    }

    fun areNotificationsEnabledOnDevice(context: Context): Boolean {
        val notificationManager = NotificationManagerCompat.from(context)
        var areEnabled = notificationManager.areNotificationsEnabled()

        if (areEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            areEnabled = notificationManager.notificationChannels.firstOrNull { channel ->
                channel.importance == NotificationManager.IMPORTANCE_NONE
            } == null
        }
        return areEnabled
    }

    fun securityUpdate(context: Context) {
        try {
            ProviderInstaller.installIfNeeded(context)
        } catch (exc: Exception) {
            Log.e(TAG, "GMS Error while trying to update SecurityProvider of GooglePlay.")
        }
    }

}