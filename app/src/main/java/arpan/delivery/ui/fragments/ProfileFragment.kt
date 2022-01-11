package arpan.delivery.ui.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import arpan.delivery.R
import arpan.delivery.ui.auth.PhoneAuthActivity
import arpan.delivery.ui.home.HomeActivity
import arpan.delivery.utils.createProgressDialog
import arpan.delivery.utils.showToast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.shashank.sony.fancytoastlib.FancyToast
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.fragment_cart.view.*
import kotlinx.android.synthetic.main.fragment_profile.view.*
import kotlinx.android.synthetic.main.fragment_profile.view.floating_action_button

class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var firebaseFirestore: FirebaseFirestore
    private var userNameOld = ""
    private var userPhoneOld = ""
    private var userAddressOld = ""
    private var IMAGE_URI = Uri.parse("")
    private lateinit var progressDialog: Dialog
    private lateinit var mainView : View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (view.context as HomeActivity).titleActionBarTextView.text = getString(R.string.my_profile_page_title)
        (view.context as HomeActivity).deleteItemsFromCart.visibility = View.GONE
        (view.context as HomeActivity).img_cart_icon.visibility = View.VISIBLE
        mainView = view
        progressDialog = view.context.createProgressDialog()
        firebaseFirestore = FirebaseFirestore.getInstance()
        if(FirebaseAuth.getInstance().currentUser==null){
            view.linearLayout.visibility = View.GONE
            view.logoutbutton.visibility = View.GONE
            view.linearLayout2.visibility = View.VISIBLE
            view.floating_action_button.setOnClickListener {
                val phoneNumberText = view.edt_phone_number.text.toString()
                if(phoneNumberText.isEmpty()){
                    view.context.showToast(getString(R.string.provide_phone_number_error_text), FancyToast.ERROR)
                }else{
                    val intentToOtpPage = Intent(view.context, PhoneAuthActivity::class.java)
                    intentToOtpPage.putExtra("phone_number_from_main_act", phoneNumberText)
                    startActivity(intentToOtpPage)
                }
            }
        }else{
            view.linearLayout.visibility = View.VISIBLE
            view.logoutbutton.visibility = View.VISIBLE
            view.linearLayout2.visibility = View.GONE
            progressDialog.show()
            fetchFirestoreProfileData(view)
        }
    }

    private fun fetchFirestoreProfileData(view : View) {
        progressDialog.show()
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        firebaseFirestore.collection("users")
                .document(uid)
                .get().addOnCompleteListener {
                    if(it.isSuccessful){
                        if(it.result!=null){
                            userNameOld = it.result!!.getString("name").toString()
                            userPhoneOld = it.result!!.getString("phone").toString()
                            userAddressOld = it.result!!.getString("address").toString()
                            view.editTextTextPersonName.setText(userNameOld)
                            view.editTextTextAddress.setText(userAddressOld)
                            view.editTextTextPhone.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber!!.toString())
                            if(it.result!!.getString("profile_image").toString() != ""){
                                val storageReference = FirebaseStorage.getInstance().reference
                                        .child("user")
                                        .child(uid)
                                        .child("profile_image")

                                Glide.with(this)
                                        .load(storageReference)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .centerCrop()
                                        .override(300,300)
                                        .placeholder(R.drawable.loading_image_glide)
                                    .into(view.registerImageView)


                                val radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70f, resources.displayMetrics)
                                view.registerImage.radius = radius
                            }
                        }
                    }else {
                        it.exception!!.printStackTrace()
                    }
                    progressDialog.dismiss()
                    initiateTextChangeListeners(view)
                    initiateOnButtonClick(view)
                }
        }

    private fun startUpdateWithImage(address: String, userName: String, phoneNumber: String) {
        progressDialog.show()
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseStorage.getInstance()
                .reference
                .child("user")
                .child(uid)
                .child("profile_image")
                .putFile(IMAGE_URI)
                .addOnSuccessListener {
                    val map = HashMap<String,String>()
                    map["name"] = userName
                    map["phone"] = phoneNumber
                    map["profile_image"] = "profile_image"
                    map["address"] = address
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .update(map as Map<String, Any>)
                            .addOnSuccessListener {
                                progressDialog.dismiss()
                                userNameOld = userName
                                userPhoneOld = phoneNumber
                                userAddressOld = address
                                IMAGE_URI = Uri.parse("")
                                Toast.makeText(context, getString(R.string.update_success), Toast.LENGTH_SHORT).show()
                            }
                }
    }

    private fun startUpdateWithoutImage(address: String, userName: String, phoneNumber: String) {
        progressDialog.show()
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val map = HashMap<String,String>()
        map["name"] = userName
        map["phone"] = phoneNumber
        map["address"] = address
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update(map as Map<String, Any>)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    userNameOld = userName
                    userPhoneOld = phoneNumber
                    userAddressOld = address
                    IMAGE_URI = Uri.parse("")
                    Toast.makeText(context, getString(R.string.update_success), Toast.LENGTH_SHORT).show()
                }
    }

    private fun initiateOnButtonClick(view : View) {
        view.registerImageView.setOnClickListener {
            ImagePicker.with(this)
                    .cropSquare()
                    .compress(60)
                    .maxResultSize(512, 512)	//Final image resolution will be less than 1080 x 1080(Optional)
                    .start()
        }
        view.saveButton.setOnClickListener {
            if(
                    view.editTextTextPersonName.text.toString()==userNameOld &&
                    view.editTextTextPhone.text.toString()==userPhoneOld &&
                    view.editTextTextAddress.text.toString()==userAddressOld &&
                    IMAGE_URI.toString().isEmpty()
            ){
                view.saveButton.isEnabled = false
            }else{
                view.saveButton.isEnabled = false
                if(IMAGE_URI.toString().isEmpty()){
                    startUpdateWithoutImage(view.editTextTextAddress.text.toString(),
                            view.editTextTextPersonName.text.toString(),
                            view.editTextTextPhone.text.toString())
                }else{
                    startUpdateWithImage(view.editTextTextAddress.text.toString(),
                            view.editTextTextPersonName.text.toString(),
                            view.editTextTextPhone.text.toString())
                }
            }
        }
    }

    private fun initiateTextChangeListeners(view : View) {
        view.editTextTextPersonName.doOnTextChanged { text, start, before, count ->
            view.saveButton.isEnabled = text!=userNameOld
        }
        view.editTextTextPhone.doOnTextChanged { text, start, before, count ->
            view.saveButton.isEnabled = text!=userPhoneOld
        }
        view.editTextTextAddress.doOnTextChanged { text, start, before, count ->
            view.saveButton.isEnabled = text!=userAddressOld
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> {
                IMAGE_URI = data!!.data
                mainView.registerImageView.setImageURI(IMAGE_URI)
                val radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70f, resources.displayMetrics)
                mainView.registerImage.radius = radius
                mainView.saveButton.isEnabled = true
            }
            ImagePicker.RESULT_ERROR -> {
                Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(context, getString(R.string.task_cancelled), Toast.LENGTH_SHORT).show()
            }
        }
    }

}