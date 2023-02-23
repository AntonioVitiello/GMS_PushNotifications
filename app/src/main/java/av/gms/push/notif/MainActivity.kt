package av.gms.push.notif

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

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

        PushUtils.securityUpdate(this)
        handleIntent(intent)
        initPushNotifications()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIntent(intent)
            setIntent(intent)
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
            PushUtils.getFid { fid: String ->
                this.fid = fid
                Log.d(TAG, "GMS_DEBUG FID=[$fid]")
            }
            PushUtils.getDeviceTokenAsync { token: String ->
                currentPushToken = token
                Log.d(TAG, "GMS_DEBUG PushToken:\n[$token]")
            }
        } catch (e: Exception) {
            Log.e(TAG, "GMS_DEBUG getToken failed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

//        PushUtils.deleteToken {
//            currentPushToken = null
//        }
    }

}