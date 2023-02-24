package av.gms.push.notif

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Firebase console:
 * https://console.firebase.google.com
 * project name: GMS Push Notifications
 */
class MainActivity : AppCompatActivity() {
    //Firebase Push Token
    private var currentPushToken: String? = null

    //Firebase installation ID
    private var fid: String? = null

    private val mPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onPermissionResult(granted)
    }

    companion object {
        const val TAG = "MainActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handleIntent(intent)
        initComponents()
    }

    private fun initComponents() {
        //Update SecurityProvider of GooglePlay
        PushUtils.securityUpdate(this)

        //Start Push Notification (request permission if first time)
        permissionButton.setOnClickListener { checkPushPermission() }
        initNotificationButton.setOnClickListener { initPushNotifications() }
        delNotificationButton.setOnClickListener { deletePushNotifications() }
    }

    /**
     * Permission for Push Notifications is only necessary for API level >= 33 (TIRAMISU)
     */
    private fun checkPushPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                initPushNotifications()
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showRequestPermissionRationale()
            } else {
                mPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            initPushNotifications()
        }
    }

    private fun onPermissionResult(permissionGranted: Boolean) {
        if (permissionGranted) {
            initPushNotifications()
        } else {
            AlertDialog.Builder(this).apply {
                setTitle("Alert")
                setMessage("Notification permission is required on Android 13 and higer.\nNotifications are disabled.")
                setIcon(android.R.drawable.ic_dialog_alert)
                setPositiveButton(android.R.string.ok, null)
            }.show()
        }
    }

    private fun showRequestPermissionRationale() {
        AlertDialog.Builder(this).apply {
            setTitle("Notification Permission")
            setMessage("From this Android version need permission to receive notifications.\nPlease allow notification permission from settings.")
            setIcon(android.R.drawable.ic_dialog_alert)
            setPositiveButton(android.R.string.ok) { dialog, which ->
                PushUtils.openNotificationsSettings(this@MainActivity)
            }
            setNegativeButton("Cancel", null)
        }.show()
    }

    /**
     * GSM Push Notifications Permission is required only on Android 13 (API level 33: TIRAMISU) and higher.
     */
    private fun showPermissionGranted() {
        val idMsg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                initPushNotifications()
                R.string.push_permission_granted
            } else {
                resetCurrentState()
                showCurrentState()
                R.string.push_permission_denied
            }
        } else { //no need permission
            R.string.push_permission_no_need
        }
        hasPermissionText.setText(idMsg)
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
            bundle.keySet()?.forEach { key ->
                val value = bundle.getString(key)
                Log.i(TAG, "Bundle key-value data: [$key] -> [$value]")
            }
        }
    }

    fun initPushNotifications() {
        try {
            PushUtils.getFid { fid: String ->
                this.fid = fid
                showCurrentState()
                Log.d(TAG, "GMS_DEBUG FID=[$fid]")
            }
            PushUtils.getDeviceTokenAsync { token: String ->
                currentPushToken = token
                showCurrentState()
                Toast.makeText(this, "Push Notifications initialized!", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "GMS_DEBUG PushToken:\n[$token]")
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Push Notifications error!", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "GMS_DEBUG getToken failed", e)
        }
    }

    override fun onResume() {
        super.onResume()
        showPermissionGranted()
    }

    private fun deletePushNotifications() {
        if(fid != null || currentPushToken != null) {
            PushUtils.deleteToken {
                resetCurrentState()
                showCurrentState()
            }
        } else {
            Log.i(TAG, "GMS_DEBUG PushNotifications not active.")
        }
    }

    private fun resetCurrentState() {
        fid = null
        currentPushToken = null
    }

    private fun showCurrentState() {
        fidText.text = fid
        pushTokenText.text = currentPushToken
    }

}