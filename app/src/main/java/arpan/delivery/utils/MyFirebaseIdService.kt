package arpan.delivery.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import arpan.delivery.R
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
        db.collection("users").document(uid).update(token)
    }

    private fun getRegistrationTokens(uid: String) {
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

    companion object {
        private const val TAG = "MyFirebaseIdService"
    }
}