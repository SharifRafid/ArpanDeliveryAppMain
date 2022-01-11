package arpan.delivery.ui.auth

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import arpan.delivery.R
import arpan.delivery.ui.home.HomeActivity
import arpan.delivery.utils.showToast
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.shashank.sony.fancytoastlib.FancyToast
import kotlinx.android.synthetic.main.activity_phone_auth.*
import java.util.*
import java.util.concurrent.TimeUnit


class PhoneAuthActivity : AppCompatActivity() {

    private val TAG = "PhoneAuthActivity"
    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var callbacks : PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var storedVerificationId = ""
    private lateinit var resendToken : PhoneAuthProvider.ForceResendingToken
    private var phoneNumberGlobal = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLanguageDefault()
        setContentView(R.layout.activity_phone_auth)
        setTheme(R.style.Theme_ArpanDelivery)
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
        baseContext.resources.updateConfiguration(
            config,
            baseContext.resources.displayMetrics
        )
    }

    private fun initLogics() {
        if(!intent.getStringExtra("phone_number_from_main_act").isNullOrEmpty()){
            edt_phone_number.setText(intent.getStringExtra("phone_number_from_main_act"))
        }

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted:$credential")
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w(TAG, "onVerificationFailed", e)
                progressSendOtpFinal.visibility = View.GONE
                buttonSendOtpFinal.visibility = View.VISIBLE
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> showToast(
                        getString(R.string.entered_a_wrong_otp_toast),
                        FancyToast.ERROR
                    )
                    is FirebaseTooManyRequestsException -> showToast(
                        getString(R.string.we_are_no_longer_accepting_phones_toast),
                        FancyToast.ERROR
                    )
                    else -> showToast(
                        getString(R.string.phone_number_verification_failed_toast),
                        FancyToast.ERROR
                    )
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "onCodeSent:$verificationId")
                storedVerificationId = verificationId
                resendToken = token
                implementCodeSentLogic()
            }
        }
    }

    private fun implementCodeSentLogic() {
        sendCodeLinear.visibility = View.GONE
        enterCodeLinear.visibility = View.VISIBLE
        otpDidntReciveText.visibility = View.VISIBLE
        buttonEnterOtpFinal.isEnabled = false
        otp_view.setText("")
        otp_view.setOtpCompletionListener {
            hideKeyboard(this)
            val codeSent = otp_view.text.toString()
            if(codeSent.isEmpty()){
                this.showToast(
                    getString(R.string.enter_otp_failed_sent_to_your_device),
                    FancyToast.ERROR
                )
            }else{
                progressEnterOtpFinal.visibility = View.VISIBLE
                buttonEnterOtpFinal.visibility = View.GONE
                checkCodeAndLogin(codeSent)
            }
        }
        object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                buttonEnterOtpFinal.text = "( "+millisUntilFinished / 1000+" )"
            }

            override fun onFinish() {
                buttonEnterOtpFinal.isEnabled = true
                buttonEnterOtpFinal.text = "পেস্ট করুন"
            }
        }.start()
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                otpDidntReciveText.text = getString(R.string.try_again_after)+" "+millisUntilFinished / 1000+" "+ getString(R.string.this_many_seconds)
            }

            override fun onFinish() {
                otpDidntReciveText.visibility = View.GONE
                buttonResendOtpFinal.visibility = View.VISIBLE
                buttonResendOtpFinal.setOnClickListener{
                    val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber("+88$phoneNumberGlobal")       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this@PhoneAuthActivity)                 // Activity (for callback binding)
                        .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)
                    buttonResendOtpFinal.visibility = View.GONE
                    otpDidntReciveText.visibility = View.GONE
                }
            }
        }.start()
    }

    fun hideKeyboard(activity: Activity) {
        val imm: InputMethodManager = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun signInWithPhoneAuthCredential(phoneAuthCredential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(phoneAuthCredential)
                .addOnCompleteListener {
                    if(it.isSuccessful){
                        if(firebaseAuth.currentUser!=null){
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(firebaseAuth.currentUser!!.uid)
                                .get().addOnCompleteListener { task->
                                    if(!task.result!!.exists()){
                                        val map = HashMap<String, String>()
                                        map["name"] = ""
                                        map["phone"] = ""
                                        map["profile_image"] = ""
                                        map["address"] = ""
                                        map["registration_token"] = ""
                                        FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(firebaseAuth.currentUser!!.uid)
                                            .set(map)
                                    }
                                    redirectToHomeActivity()
                                }
                        }else{
                            sendCodeLinear.visibility = View.GONE
                            enterCodeLinear.visibility = View.VISIBLE
                            progressEnterOtpFinal.visibility = View.GONE
                            buttonEnterOtpFinal.visibility = View.VISIBLE
                            otp_view.setText("")
                            showToast(
                                getString(R.string.failed_to_sign_in_with_phone_number),
                                FancyToast.ERROR
                            )
                        }
                    }else{
                        sendCodeLinear.visibility = View.GONE
                        enterCodeLinear.visibility = View.VISIBLE
                        progressEnterOtpFinal.visibility = View.GONE
                        buttonEnterOtpFinal.visibility = View.VISIBLE
                        otp_view.setText("")
                        it.exception!!.printStackTrace()
                        showToast(
                            getString(R.string.failed_to_sign_in_with_phone_number),
                            FancyToast.ERROR
                        )
                    }
                }
    }

    private fun redirectToHomeActivity() {
        getSharedPreferences("APP_SETTINGS", MODE_PRIVATE)
            .edit().putBoolean("FIRST_LAUNCH", false).apply()
        val i = Intent(this@PhoneAuthActivity, HomeActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
        finish()
    }

    fun onSendCodeButtonClick(view: View){
        val phoneNumber = edt_phone_number.text.toString()
        if(phoneNumber.isEmpty()){
            this.showToast(getString(R.string.provide_phone_number_error_text), FancyToast.ERROR)
        }else{
            progressSendOtpFinal.visibility = View.VISIBLE
            buttonSendOtpFinal.visibility = View.GONE
            phoneNumberGlobal = phoneNumber
            startCodeSendingProcess(phoneNumber)
        }
    }

    fun onSubmitCodeButtonClick(view: View){
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        var pasteData = ""

        // If it does contain data, decide if you can handle the data.

        // If it does contain data, decide if you can handle the data.
        if (!clipboard.hasPrimaryClip()) {
        } else if (!clipboard.getPrimaryClipDescription()!!.hasMimeType(
                MIMETYPE_TEXT_PLAIN
            )
        ) {

            // since the clipboard has data but it is not plain text
        } else {

            //since the clipboard contains plain text.
            val item: ClipData.Item = clipboard.getPrimaryClip()!!.getItemAt(0)

            // Gets the clipboard as text.
            pasteData = item.text.toString()
            otp_view.setText(pasteData)
        }

    }

    private fun checkCodeAndLogin(codeSent: String) {
        if(storedVerificationId.isNullOrEmpty()){
            progressEnterOtpFinal.visibility = View.GONE
            buttonEnterOtpFinal.visibility = View.VISIBLE
            this.showToast(getString(R.string.try_again_to_send_code_error), FancyToast.ERROR)
        }else{
            val credential = PhoneAuthProvider.getCredential(storedVerificationId, codeSent)
            signInWithPhoneAuthCredential(credential)
        }
    }

    private fun startCodeSendingProcess(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber("+88$phoneNumber")       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(this)                 // Activity (for callback binding)
                .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
                .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}