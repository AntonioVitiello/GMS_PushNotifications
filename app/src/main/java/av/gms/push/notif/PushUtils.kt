package av.gms.push.notif

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.security.ProviderInstaller
import com.google.android.gms.tasks.Tasks
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Created by Antonio Vitiello on 23/02/2023.
 */
class PushUtils {
    companion object {

        /**
         * return Firebase Installation Id: FID
         */
        fun getFid(task: (String) -> Unit) {
            try {
                FirebaseInstallations.getInstance().id.addOnSuccessListener { fid: String ->
                    task(fid)
                    Log.d(MainActivity.TAG, "GMS_DEBUG FID=$fid")
                }
            } catch (exc: Exception) {
                Log.e(MainActivity.TAG, "GMS_DEBUG failed getting FID", exc)
            }
        }

        /**
         * Must not be called on the main application thread!
         */
        fun getDeviceToken(): String {
            return Tasks.await(FirebaseMessaging.getInstance().token).also { token: String ->
                Log.d(MainActivity.TAG, "GMS_DEBUG getDeviceToken:$token")
            }
        }

        fun getDeviceTokenAsync(task: (String) -> Unit) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token: String ->
                task(token)
                Log.d(MainActivity.TAG, "GMS_DEBUG getDeviceTokenAsync:$token")
            }
        }

        fun deleteToken(task: () -> Unit) {
            try {
                FirebaseMessaging.getInstance().deleteToken()
                FirebaseInstallations.getInstance().delete()
                task.invoke()
                Log.i(MainActivity.TAG, "GMS_DEBUG PushToken deleted successfully")
            } catch (exc: Exception) {
                Log.e(MainActivity.TAG, "GMS_DEBUG Delete PushToken failed", exc)
            }
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
                Log.e(MainActivity.TAG, "GMS_DEBUG Error while trying to update SecurityProvider of GooglePlay.")
            }
        }

    }
}