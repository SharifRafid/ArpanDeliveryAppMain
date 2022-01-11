package arpan.delivery.utils

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import arpan.delivery.R
import com.shashank.sony.fancytoastlib.FancyToast
import kotlinx.android.synthetic.main.dialog_alert_layout_main.view.*
import java.util.*


fun Context.showToast(message: String, status: Int){
    val toast = FancyToast.makeText(this, message, FancyToast.LENGTH_SHORT, status, false)
    toast.setGravity(Gravity.BOTTOM, 0,200 )
    toast.show()
}

fun Context.createProgressDialog() : Dialog {
    val dialog = AlertDialog.Builder(this)
        .setView(LayoutInflater.from(this)
            .inflate(R.layout.dialog_progress_layout_main, null))
        .create()
    dialog.setCancelable(false)
    dialog.setCanceledOnTouchOutside(false)
    dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
    return dialog
}

fun Context.createAlertDialogMain(title: String, message: String, yesButtonText: String, noButtonText: String) : Dialog {
    val view = LayoutInflater.from(this).inflate(R.layout.dialog_alert_layout_main, null)
    view.titleTextView.text = title
    view.messageTextView.text = message
    view.btnYesDialogAlertMain.text = yesButtonText
    view.btnNoDialogAlertMain.text = noButtonText
    val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()
    dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
    return dialog
}

fun Context.createLanguageAlertDialogMain() : Dialog {
    val view = LayoutInflater.from(this).inflate(R.layout.dialog_alert_layout_main, null)
    view.titleTextView.text = "ভাষা নির্বাচন"
    view.messageTextView.text = "অ্যাপটি ব্যবহার করার জন্য আপনার ভাষা সিলেক্ট করুন। এটি আপনি সেটিংস থেকে পরবর্তীতে পরিবর্তন করতে পারবেন।"
    view.btnYesDialogAlertMain.text = "বাংলা"
    view.btnNoDialogAlertMain.text = "ইংরেজি"
    val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()
    view.btnYesDialogAlertMain.setOnClickListener {
        getSharedPreferences("APP_SETTINGS", MODE_PRIVATE)
                .edit().putString("lang", "bn").apply()
        dialog.dismiss()
    }
    view.btnNoDialogAlertMain.setOnClickListener {
        getSharedPreferences("APP_SETTINGS", MODE_PRIVATE)
                .edit().putString("lang", "en").apply()
        dialog.dismiss()
    }
    return dialog
}