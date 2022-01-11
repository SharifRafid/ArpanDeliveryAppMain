package arpan.delivery.utils

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.multidex.MultiDex
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*

class MyApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}