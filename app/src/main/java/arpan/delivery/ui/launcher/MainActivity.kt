package arpan.delivery.ui.launcher

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import arpan.delivery.R
import arpan.delivery.ui.auth.PhoneAuthActivity
import arpan.delivery.ui.home.HomeActivity
import arpan.delivery.utils.createLanguageAlertDialogMain
import arpan.delivery.utils.showToast
import com.google.firebase.auth.FirebaseAuth
import com.shashank.sony.fancytoastlib.FancyToast
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLanguageDefault()
        setContentView(R.layout.activity_main)

        initLogics()
    }

    private fun setLanguageDefault() {
        val language = getSharedPreferences("APP_SETTINGS", MODE_PRIVATE)
            .getString("lang", "bn")
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        if(Build.VERSION.SDK_INT >= 17) { config.setLocale(locale); } else { config.locale = locale; }
        baseContext.resources.updateConfiguration(config,
                baseContext.resources.displayMetrics)
    }

    private fun initLogics() {
        sharedPreferences = getSharedPreferences("APP_SETTINGS", MODE_PRIVATE)
        if(checkFirstLaunch()){
            //checkLanguageSelectedStatus()
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        if(FirebaseAuth.getInstance().currentUser!=null){
            redirectToHomeActivity()
        }else{
            setTheme(R.style.Theme_ArpanDelivery)
            launchAnimation()
        }
    }

    private fun launchAnimation() {
        val mTimer1 = Timer()
        val mTt1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    mainLinearView.animate().alpha(1f).duration = 500
                }
            }
        }
        mTimer1.schedule(mTt1, 1500)
        val mTimer2 = Timer()
        val mTt2 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    icon_arpan_icon.animate().translationY(-100f).duration = 1500
                }
            }
        }
        mTimer2.schedule(mTt2, 500)
    }

    private fun checkFirstLaunch() : Boolean{
        return sharedPreferences.getBoolean("FIRST_LAUNCH", true)
    }

    private fun checkLanguageSelectedStatus() {
        if(sharedPreferences.contains("lang")){
            if(sharedPreferences.getString("lang", "bn").equals("bn")){
                Log.e("Language", "Bangla")
                //implementTheBanglaLanguage()
            }else{
                Log.e("Language", "English")
                //implementTheEnglishLanguage()
            }
        }else{
            getSharedPreferences("APP_SETTINGS", MODE_PRIVATE)
                .edit().putString("lang", "bn").apply()
            //showLanguageSelectionDialog()
        }
    }

    private fun showLanguageSelectionDialog() {
        val dialog = this.createLanguageAlertDialogMain()
        dialog.show()
        dialog.setOnDismissListener {
            checkLanguageSelectedStatus()
        }
    }

    fun onRegistrationButtonClick(view: View){
        val phoneNumberText = edt_phone_number.text.toString()
        val intentToOtpPage = Intent(this, PhoneAuthActivity::class.java)
        intentToOtpPage.putExtra("phone_number_from_main_act", phoneNumberText)
        startActivity(intentToOtpPage)
//        if(phoneNumberText.isEmpty()){
//            this.showToast(getString(R.string.provide_phone_number_error_text), FancyToast.ERROR)
//        }else{
//            val intentToOtpPage = Intent(this, PhoneAuthActivity::class.java)
//            intentToOtpPage.putExtra("phone_number_from_main_act", phoneNumberText)
//            startActivity(intentToOtpPage)
//        }
    }

    fun onNoRegisterButtonClick(view: View){
        sharedPreferences.edit().putBoolean("FIRST_LAUNCH", false).apply()
        redirectToHomeActivity()
    }

    private fun redirectToHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}