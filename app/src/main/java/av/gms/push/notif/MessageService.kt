package av.gms.push.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Antonio Vitiello on 13/02/2023.
 * vedi: C:\Android\projects\Links\SCRIGNOapp\GitLab\develop\app\src\gms\java\it\bps\scrigno\helpers\service\MessagingService.kt
 */
class MessageService : FirebaseMessagingService() {
    private lateinit var idChannel: String
//    private val notificheRepository = NotificheRepository()

    companion object {
        const val TAG = "AAA"
        private const val PAYLOAD_KEY = "payload"
        private const val NOTIFICATION_ID_KEY = "id"
        const val KEY_EXTRA_INTENT_NOTIFICATION_ID = "notification id"
        private val atomicInteger = AtomicInteger()

        fun idMsgToInt(idMsg: String?): Int {
            //eg: messageId="0:1677094539719860%8334b89d8334b89d"
            return try {
                idMsg!!.substring(2, idMsg.indexOf("%8")).toLong().mod(Int.MAX_VALUE)
            } catch (exc: Exception){
                0
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        //Create NotificationChannel and register the channel with the system
        idChannel = getString(R.string.default_notification_channel_id)
        createChannel(idChannel, getString(R.string.default_notification_channel_name))
    }

    /**
     * description is a user visible description of this channel with maximum length is 300 characters
     * the value may be truncated if it is too long.
     */
    private fun createChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                //notificationChannel.lightColor = Color.RED
                enableVibration(true)
                description = getString(R.string.user_visible_channel_description)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        try {
            logNotification(remoteMessage)

            //eg. key-value pair is:  payload = {"dettaglio":false,"id":"26531700"}
            val payload: JSONObject = JSONObject(remoteMessage.data[PAYLOAD_KEY] ?: "{}")
            Log.d(TAG, "GMS Push PAYLOAD: $payload")
            if (!payload.isNull(NOTIFICATION_ID_KEY)) {
                val notificationId = payload.getInt(NOTIFICATION_ID_KEY)
                Log.d(TAG, "GMS Push notification id: $notificationId")
            }

            //after parsing show notification in notification drawer (cassetto delle notifiche)
            val title = remoteMessage.notification?.title ?: getString(R.string.app_name)
            val body = remoteMessage.notification?.body ?: getString(R.string.default_notification_channel_name)
            val notificationId = atomicInteger.getAndIncrement()
//            val notificationId = idMsgToInt(remoteMessage.messageId)
            showNotification(title, body, notificationId)

        } catch (exc: Exception) {
            Log.e(TAG, "GMS Error: while decoding notification, remoteMessage:${remoteMessage.data}", exc)
        }
    }

    private fun logNotification(remoteMessage: RemoteMessage) {
        //with: remoteMessage.notification
        remoteMessage.notification?.let { notification ->
            Log.d(
                TAG,
                """GMS_DEBUG NOTIFICATION DATA:
                     ImageUrl: ${notification.imageUrl}
                     Title: ${notification.title}
                     TitleLocalizationKey: ${notification.titleLocalizationKey}
                     TitleLocalizationArgs: ${Arrays.toString(notification.titleLocalizationArgs)}
                     Body: ${notification.body}
                     BodyLocalizationKey: ${notification.bodyLocalizationKey}
                     BodyLocalizationArgs: ${Arrays.toString(notification.bodyLocalizationArgs)}
                     Icon: ${notification.icon}
                     Sound: ${notification.sound}
                     Tag: ${notification.tag}
                     Color: ${notification.color}
                     ClickAction: ${notification.clickAction}
                     ChannelId: ${notification.channelId}
                     Link: ${notification.link}
                      """.trimIndent()
            )
        }
        //with: remoteMessage.data
        Log.d(
            TAG, """GMS_DEBUG MESSAGE DATA:
                    data: ${remoteMessage.data}
                    messageId: ${remoteMessage.messageId}
                    messageType: ${remoteMessage.messageType}
                    from: ${remoteMessage.from}
                    to: ${remoteMessage.to}
                    priority: ${remoteMessage.priority}
                    sentTime: ${remoteMessage.sentTime}
                    senderId: ${remoteMessage.senderId}
                    ttl: ${remoteMessage.ttl}
                    """.trimIndent()
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken:\n[$token]")
//        notificheRepository.updateServerToken(token)
    }

    /**
     * Create and show notification containing the received FCM message.
     * @param messageBody FCM message body received.
     */
    private fun showNotification(title: String, messageBody: String, notificationId: Int) {

        //Create Notification Intent
        val contentIntent = Intent(this, NotificationTapActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(KEY_EXTRA_INTENT_NOTIFICATION_ID, notificationId.toString())
        }

        //Create PendingIntent that will start a new activity
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(this, notificationId, contentIntent, pendingFlags)
        val messageText = "$messageBody\n${notificationId}"

        //Create Notification for the specific channel_id and sets fields
        val builder = NotificationCompat.Builder(this, idChannel)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        //Colorized icon for Android 10 and above
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            val purple700 = ContextCompat.getColor(this, R.color.purple_700)
            builder.apply {
                setSmallIcon(R.drawable.ic_push_notification)
                setColorized(true)
                color = purple700
            }
        }

        //Post notification to be shown in the status bar
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(notificationId, builder.build())
    }

}