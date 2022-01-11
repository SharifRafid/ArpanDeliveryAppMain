package arpan.delivery.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import arpan.delivery.R
import arpan.delivery.ui.home.HomeActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*


class MyFirebaseIdService : FirebaseMessagingService() {

    var db = FirebaseFirestore.getInstance()
    var registrationTokens: List<String>? = null

    override fun onNewToken(s: String) {
        super.onNewToken(s)


        //FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        val prefs = applicationContext.getSharedPreferences("USER_PREF",
                MODE_PRIVATE)

        val uid = FirebaseAuth.getInstance().uid.toString()

        getRegistrationTokens(uid)

        if (!uid.equals("null", ignoreCase = true)) {
            if (registrationTokens != null && !registrationTokens!!.contains(s)) {
                updateToken(s, uid)
            }
        }
    }

    private fun updateToken(token: String, uid: String) {
//        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        val tokenArray: MutableMap<String, Any> = HashMap()
        tokenArray["registrationTokens"] = FieldValue.arrayUnion(token)
        addRegistrationToken(tokenArray, uid)
    }

    private fun addRegistrationToken(token: Map<String, Any>, uid: String) {
        if(uid.isNotEmpty()){
            db.collection("users").document(uid).update(token)
        }
    }

    private fun getRegistrationTokens(uid: String) {
        if(uid.isNotEmpty()){
            db.collection("users")
                .document(uid)
                .get()
                .addOnCompleteListener(OnCompleteListener<DocumentSnapshot?> { task ->
                    if (task.isSuccessful) {
                        val document = task.result
                        if (document!!.exists()) {
                            Log.d(TAG, "DocumentSnapshot data: " + document.data)
                            registrationTokens = document["registrationTokens"] as List<String>?
                        } else {
                            Log.d(TAG, "No such document")
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.exception)
                    }
                })
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("msg", "onMessageReceived: " + remoteMessage.data["message"])
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        for(entry in remoteMessage.data.entries){
            intent.putExtra(entry.key, entry.value.toString())
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val channelId = "Default"
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_arpan_icon_notification)
            .setContentTitle(remoteMessage.notification!!.title)
            .setContentText(remoteMessage.notification!!.body).setAutoCancel(true)
            .setContentIntent(pendingIntent)
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.N){
            builder.priority = NotificationCompat.PRIORITY_HIGH
            builder.setDefaults(Notification.DEFAULT_ALL)
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.priority = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                channelId,
                "Default channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }
        manager.notify(0, builder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseIdService"
    }
}