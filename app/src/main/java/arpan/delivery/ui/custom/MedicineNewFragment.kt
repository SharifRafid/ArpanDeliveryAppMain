package arpan.delivery.ui.custom

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import arpan.delivery.R
import arpan.delivery.data.db.CartProductEntity
import arpan.delivery.data.models.OrderItemMain
import arpan.delivery.data.models.PromoCode
import arpan.delivery.ui.home.HomeActivity
import arpan.delivery.ui.home.HomeViewModel
import arpan.delivery.utils.createProgressDialog
import arpan.delivery.utils.showToast
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.shashank.sony.fancytoastlib.FancyToast
import com.squareup.okhttp.*
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.dialog_alert_layout_main.view.*
import kotlinx.android.synthetic.main.fragment_medicine_new.view.*
import kotlinx.android.synthetic.main.fragment_medicine_new.view.addPromoCode
import kotlinx.android.synthetic.main.fragment_medicine_new.view.applyPromoCodeLinear
import kotlinx.android.synthetic.main.fragment_medicine_new.view.apply_promo_code
import kotlinx.android.synthetic.main.fragment_medicine_new.view.bkash_charge_note
import kotlinx.android.synthetic.main.fragment_medicine_new.view.edt_coupon_code
import kotlinx.android.synthetic.main.fragment_medicine_new.view.hidePromoCodeEnterStage
import kotlinx.android.synthetic.main.fragment_medicine_new.view.imageView
import kotlinx.android.synthetic.main.fragment_medicine_new.view.promoCodeAppliedLinear
import kotlinx.android.synthetic.main.fragment_medicine_new.view.promoCodeAppliedRemoveText
import kotlinx.android.synthetic.main.fragment_medicine_new.view.promoCodeAppliedText
import kotlinx.android.synthetic.main.fragment_medicine_new.view.promoCodeLinear
import kotlinx.android.synthetic.main.fragment_medicine_new.view.radioGroup
import kotlinx.android.synthetic.main.fragment_medicine_new.view.spinner_2
import kotlinx.android.synthetic.main.fragment_medicine_new.view.txt_address
import kotlinx.android.synthetic.main.fragment_medicine_new.view.txt_details
import kotlinx.android.synthetic.main.fragment_medicine_new.view.txt_name
import kotlinx.android.synthetic.main.fragment_medicine_new.view.txt_note
import kotlinx.android.synthetic.main.fragment_medicine_new.view.txt_number
import kotlinx.android.synthetic.main.fragment_medicine_new.view.txt_place_order
import kotlinx.android.synthetic.main.fragment_medicine_new.view.txt_price
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MedicineNewFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var deliveryLocations = ArrayList<String>()
    private var deliveryCharges = ArrayList<Int>()
    private lateinit var contextMain: Context
    private lateinit var viewMain : View
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var dialog : Dialog
    private var promoCodeActive = false
    private var promoCode = PromoCode()

    private var imagePath : Uri = Uri.parse("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_medicine_new, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initVars(view)
        initLogics(view)
        initSavedDataPlacement(view)
        initSavedDataListeners(view)
        initSpinnersLocations(view)
        initRadioGroup(view)
        initiatePromoCodeLogic(view)
    }

    private fun initSpinnersLocations(view: View) {
        for(item in homeViewModel.getLocationArrayPickDrop()) {
            deliveryCharges.add(item.deliveryCharge)
            deliveryLocations.add(item.locationName)
        }
        val adapter = ArrayAdapter(view.context, R.layout.custom_spinner_view, deliveryLocations)
        adapter.setDropDownViewResource(R.layout.custom_spinner_item_view)
        view.spinner_2.adapter = adapter
        view.spinner_2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view2: View?,
                position: Int,
                id: Long
            ) {
                setPriceTotalOnView(view)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                view.spinner_2.setSelection(0)
            }
        }
    }

    private fun setPriceTotalOnView(view: View) {
        if(promoCodeActive){
            if(homeViewModel.getLocationArrayPickDrop()[view.spinner_2.selectedItemPosition].locationName.trim()=="মাগুরা সদর"){
                if(promoCode.discountPrice>homeViewModel.getLocationArrayPickDrop()[view.spinner_2.selectedItemPosition].deliveryCharge){
                    view.txt_price.text = "ডেলিভারি চার্জঃ 0 টাকা"
                }else{
                    view.txt_price.text = "ডেলিভারি চার্জঃ ${deliveryCharges[view.spinner_2.selectedItemPosition] - promoCode.discountPrice} টাকা"
                }
            }else{
                view.context.showToast("প্রোমো কোডটি শুধু মাত্র মাগুরা সদর এর জন্য প্রযোজ্য হবে।", FancyToast.ERROR)
                view.txt_price.text = "ডেলিভারি চার্জঃ ${deliveryCharges[view.spinner_2.selectedItemPosition]} টাকা"
            }
        }else{
            view.txt_price.text = "ডেলিভারি চার্জঃ ${deliveryCharges[view.spinner_2.selectedItemPosition]} টাকা"
        }
    }

    private fun initSavedDataListeners(view: View) {
        view.txt_details.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                sharedPreferences.edit().putString("medicine_data_edt",s.toString())
                    .apply()
            }
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {}
            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
            }
        })
        view.txt_pharmacy.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                sharedPreferences.edit().putString("medicine_data_pharmacy",s.toString())
                    .apply()
            }
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {}
            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
            }
        })
        view.txt_note.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                sharedPreferences.edit().putString("medicine_data_note",s.toString())
                    .apply()
            }
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {}
            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
            }
        })
        view.imageView.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
                .start()
        }
    }

    private fun initSavedDataPlacement(view: View) {
        view.txt_details.setText(sharedPreferences.getString("medicine_data_edt",""))
        view.txt_pharmacy.setText(sharedPreferences.getString("medicine_data_pharmacy",""))
        view.txt_note.setText(sharedPreferences.getString("medicine_data_note",""))
        imagePath = Uri.parse(sharedPreferences.getString("photo",""))
        if(imagePath.toString().isNotEmpty()){
            view.imageView.setImageURI(imagePath)
        }
    }

    private fun initLogics(view: View) {
        var snapshot = (view.context as HomeActivity).dataSnapshotOrderTakingTime
        val startTime = snapshot.child("start_time").value.toString()
        val endTime = snapshot.child("end_time").value.toString()
        val over_time_orders = snapshot.child("over_time_orders").value.toString()
        if(over_time_orders == "yes"){
            view.txt_place_order.visibility = View.VISIBLE
        }else{
            val string1 = "${startTime}:00"
            val time1 = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).parse(string1)
            val calendar1 = Calendar.getInstance()
            calendar1.time = time1
            calendar1.add(Calendar.DATE, 1)
            val string2 = "${endTime}:00"
            val time2 = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).parse(string2)
            val calendar2 = Calendar.getInstance()
            calendar2.time = time2
            calendar2.add(Calendar.DATE, 1)
            val someRandomTime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(Calendar.getInstance().time)
            val date = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).parse(someRandomTime)
            val calendar3 = Calendar.getInstance()
            calendar3.time = date
            calendar3.add(Calendar.DATE, 1)
            Log.e("C3", date.toString())
            Log.e("C2", time2.toString())
            Log.e("C1", time1.toString())
            val x = calendar3.time
            if(x.after(calendar1.time) && x.before(calendar2.time)) {
                view.txt_place_order.visibility = View.VISIBLE
            }else{
                view.context.showToast(getString(R.string.shgop_order_time_ehon_bodro_ache), FancyToast.ERROR)
                view.txt_place_order.visibility = View.GONE
            }
        }

        (view.context as HomeActivity).titleActionBarTextView.text = getString(R.string.medicine_page_title)
        (view.context as HomeActivity).deleteItemsFromCart.visibility = View.GONE
        (view.context as HomeActivity).img_cart_icon.visibility = View.VISIBLE
        view.txt_name.setText((view.context as HomeActivity).userNameFromProfile)
        view.txt_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber!!.toString())
        view.txt_address.setText((view.context as HomeActivity).userAddressFromProfile)
        view.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            if(checkedId == R.id.rb1){
                view.bkash_charge_note.visibility = View.VISIBLE
            }else{
                view.bkash_charge_note.visibility = View.GONE
            }
        }
        view.txt_place_order.setOnClickListener {
            val userName = view.txt_name.text.toString()
            val userNumber = view.txt_number.text.toString()
            val userAddress = view.txt_address.text.toString()
            val userNote = view.txt_note.text.toString()
            if(userName.isNotEmpty()&&userNumber.isNotEmpty()&&userAddress.isNotEmpty()){
                if (imagePath.toString().isNotEmpty() ||
                    view.txt_details.text.isNotEmpty()
                ) {
                    val dialogAskingView = LayoutInflater.from(view.context)
                        .inflate(R.layout.dialog_alert_layout_main, null)
                    val dialog = AlertDialog.Builder(view.context)
                        .setView(dialogAskingView).create()
                    dialogAskingView.btnNoDialogAlertMain.text = getString(R.string.no)
                    dialogAskingView.btnYesDialogAlertMain.text = getString(R.string.ok_text)
                    dialogAskingView.titleTextView.text = "অর্ডার প্লেস করবেন ?"
                    dialogAskingView.messageTextView.text = "আপনি অর্ডার টি কনফার্ম করতে চলেছেন, আপনি কি নিশ্চিত ?"
                    dialogAskingView.btnNoDialogAlertMain.setOnClickListener {
                        dialog.dismiss()
                    }
                    dialogAskingView.btnYesDialogAlertMain.setOnClickListener {
                        placeOrder(view.txt_pharmacy.text.toString(), view.txt_details.text.toString(), imagePath, userName, userNumber, userAddress, userNote)
                        dialog.dismiss()
                    }
                    dialog.show()
                } else {
                    FancyToast.makeText(
                        context, getString(R.string.fill_all_the_fields),
                        FancyToast.LENGTH_SHORT, FancyToast.ERROR, false
                    ).show()
                }
            }else{
                view.context.showToast(getString(R.string.no_products), FancyToast.ERROR)
            }
        }
    }

    private fun initVars(view: View) {
        contextMain = view.context
        viewMain = view
        sharedPreferences = view.context.getSharedPreferences("medicine_orders_data", Context.MODE_PRIVATE)
        homeViewModel = activity?.let { ViewModelProvider(it).get(HomeViewModel::class.java) }!!
        dialog = view.context.createProgressDialog()
    }

    private fun initiatePromoCodeLogic(view: View) {
        view.addPromoCode.setOnClickListener {
            view.applyPromoCodeLinear.visibility = View.GONE
            view.promoCodeLinear.visibility = View.VISIBLE
            view.promoCodeAppliedLinear.visibility = View.GONE
        }
        view.hidePromoCodeEnterStage.setOnClickListener {
            view.edt_coupon_code.setText("")
            view.applyPromoCodeLinear.visibility = View.VISIBLE
            view.promoCodeLinear.visibility = View.GONE
            view.promoCodeAppliedLinear.visibility = View.GONE
        }
        view.apply_promo_code.setOnClickListener {
            if(view.edt_coupon_code.text.isNotEmpty()){
                dialog.show()
                FirebaseDatabase.getInstance().reference
                    .child("PROMO_CODES")
                    .get().addOnCompleteListener {
                        if(it.isSuccessful){
                            val promoCodesArray = ArrayList<PromoCode>()
                            var errorMessage = getString(R.string.promo_code_invalid)
                            for(item in it.result!!.children){
                                promoCodesArray.add(item.getValue(PromoCode::class.java)!!)
                            }
                            val filteredArray =
                                promoCodesArray.filter { promoCode2 ->
                                    if(promoCode2.promoCodeName.equals(view.edt_coupon_code.text.toString(), ignoreCase = true)){
                                        if(promoCode2.active){
                                            if(promoCode2.remainingUses!=0){
                                                if(promoCode2.validityOfCode>System.currentTimeMillis()){
                                                    if(promoCode2.deliveryDiscount && !promoCode2.shopBased){
                                                        if(promoCode2.onceForOneUser){
                                                            if(!promoCode2.userIds.contains(FirebaseAuth.getInstance().currentUser!!.uid)){
                                                                true
                                                            }else{
                                                                errorMessage = "এই প্রমো কোডটি এক জন ইউজার একবার ই মাত্র ব্যবহার করতে পারবে। "
                                                                false
                                                            }
                                                        }else{
                                                            true
                                                        }
                                                    }else{
                                                        errorMessage = "এই প্রমো কোডটি এই অর্ডারের ক্ষেত্রে প্রযোজ্য নয়। "
                                                        false
                                                    }
                                                }else{
                                                    errorMessage = getString(R.string.time_of_promo_code)
                                                    false
                                                }
                                            }else{
                                                errorMessage = getString(R.string.already_used_max)
                                                false
                                            }
                                        }else{
                                            errorMessage = getString(R.string.not_active_try_another)
                                            false
                                        }
                                    }else{
                                        false
                                    }
                                }
                            if(filteredArray.isEmpty()){
                                dialog.dismiss()
                                view.context.showToast(errorMessage, FancyToast.ERROR)
                            }else{
                                promoCodeActive = true
                                promoCode = filteredArray[0]
                                view.promoCodeLinear.visibility = View.GONE
                                view.promoCodeAppliedLinear.visibility =  View.VISIBLE
                                view.applyPromoCodeLinear.visibility = View.GONE

                                view.promoCodeAppliedText.text = "আপনার প্রোমো কোডটি অ্যাপ্লাই করা হয়েছে।"

                                view.promoCodeAppliedRemoveText.setOnClickListener {
                                    promoCodeActive = false
                                    promoCode = PromoCode()
                                    view.applyPromoCodeLinear.visibility = View.VISIBLE
                                    view.promoCodeLinear.visibility = View.GONE
                                    view.promoCodeAppliedLinear.visibility = View.GONE
                                    view.edt_coupon_code.setText("")
                                    setPriceTotalOnView(view)
                                }
                                setPriceTotalOnView(view)
                                dialog.dismiss()
                            }
                        }else{
                            it.exception!!.printStackTrace()
                        }
                    }
            }
        }
    }

    private fun initRadioGroup(view: View) {
        view.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            if(checkedId == R.id.rb1){
                view.bkash_charge_note.visibility = View.VISIBLE
            }else{
                view.bkash_charge_note.visibility = View.GONE
            }
            setPriceTotalOnView(view)
        }
    }

    private fun placeOrderFinalUpload(view: View, orderItemMain: OrderItemMain) {
        FirebaseDatabase.getInstance().reference.child("orderNumberNew")
            .child("ON")
            .get().addOnCompleteListener { task ->
                if(task.isSuccessful){
                    if(task.result!!.value==null){
                        orderItemMain.orderId = "ARP1001"
                        FirebaseDatabase.getInstance().reference.child("orderNumberNew")
                            .child("ON").setValue("1001")
                    }else{
                        orderItemMain.orderId = "ARP"+(task.result!!.value.toString().toInt()+1)
                        FirebaseDatabase.getInstance().reference.child("orderNumberNew")
                            .child("ON").setValue((task.result!!.value.toString().toInt()+1).toString())
                    }


                    if(promoCodeActive){
                        FirebaseDatabase.getInstance().reference
                            .child("PROMO_CODES")
                            .child(promoCode.key).get().addOnCompleteListener { it2 ->
                                if(it2.isSuccessful){
                                    val newPromoCode = it2.result.getValue(PromoCode::class.java)!!
                                    newPromoCode.remainingUses -= 1
                                    if(newPromoCode.onceForOneUser){
                                        newPromoCode.userIds += (","+FirebaseAuth.getInstance().currentUser!!.uid+",")
                                    }
                                    FirebaseDatabase.getInstance().reference
                                        .child("PROMO_CODES")
                                        .child(promoCode.key)
                                        .setValue(newPromoCode)
                                }
                                // HERE SAME

                                FirebaseFirestore.getInstance().collection("users")
                                    .document(FirebaseAuth.getInstance().currentUser!!.uid)
                                    .collection("users_order_collection")
                                    .add(orderItemMain)
                                    .addOnCompleteListener {
                                        if(it.isSuccessful){
                                            view.context.showToast(getString(R.string.order_placed_successfully), FancyToast.SUCCESS)
                                            sendNotification(
                                                FirebaseAuth.getInstance().currentUser!!.uid,
                                                "নতুন অর্ডার ${orderItemMain.orderId}",
                                                "আপনি একটি নতুন অর্ডার পেয়েছেন দ্রুত অর্ডার টি কনফার্ম করুন। ধন্যবাদ ।",
                                                it.result!!.id
                                            )
                                            dialog.dismiss()
                                            sharedPreferences.edit().clear().apply()
                                            val bundle = Bundle()
                                            bundle.putString("orderID",it.result.id)
                                            (view.context as HomeActivity).navController.navigate(R.id.action_homeFragment_self, bundle)
                                            (view.context as HomeActivity).navController.navigate(R.id.orderHistoryFragment, bundle)
                                        }else{
                                            dialog.dismiss()
                                            it.exception!!.printStackTrace()
                                            view.context.showToast(getString(R.string.failed_order), FancyToast.ERROR)
                                        }
                                    }
                            }
                    }else{
                        // HERE SAME

                        FirebaseFirestore.getInstance().collection("users")
                            .document(FirebaseAuth.getInstance().currentUser!!.uid)
                            .collection("users_order_collection")
                            .add(orderItemMain)
                            .addOnCompleteListener {
                                if(it.isSuccessful){
                                    view.context.showToast(getString(R.string.order_placed_successfully), FancyToast.SUCCESS)
                                    sendNotification(
                                        FirebaseAuth.getInstance().currentUser!!.uid,
                                        "নতুন অর্ডার ${orderItemMain.orderId}",
                                        "আপনি একটি নতুন অর্ডার পেয়েছেন দ্রুত অর্ডার টি কনফার্ম করুন। ধন্যবাদ ।",
                                        it.result!!.id
                                    )
                                    dialog.dismiss()
                                    sharedPreferences.edit().clear().apply()
                                    val bundle = Bundle()
                                    bundle.putString("orderID",it.result.id)
                                    (view.context as HomeActivity).navController.navigate(R.id.action_homeFragment_self, bundle)
                                    (view.context as HomeActivity).navController.navigate(R.id.orderHistoryFragment, bundle)
                                }else{
                                    dialog.dismiss()
                                    it.exception!!.printStackTrace()
                                    view.context.showToast(getString(R.string.failed_order), FancyToast.ERROR)
                                }
                            }
                    }

                }else{
                    dialog.dismiss()
                    task.exception!!.printStackTrace()
                    view.context.showToast(getString(R.string.failed_order), FancyToast.ERROR)
                }
            }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK) {
            val fullPhotoUri = data!!.data
            viewMain.imageView.setImageURI(fullPhotoUri)
            sharedPreferences.edit().putString("photo",fullPhotoUri.toString()).apply()
            imagePath = fullPhotoUri!!
        }
    }

    fun sendNotification(
        userId: String,
        apititle: String,
        apibody: String,
        orderID: String
    ) {
        val mediaType: MediaType =
            MediaType.parse("application/json; charset=utf-8")
        val data: MutableMap<String, String> =
            java.util.HashMap()
        data["userId"] = userId
        data["apititle"] = apititle
        data["apibody"] = apibody
        data["orderID"] = orderID
        data["click_action"] = ".ui.order.OrdresActivity"
        val json = Gson().toJson(data)
        val body: RequestBody =
            RequestBody.create(
                mediaType,
                json
            )
        val request: Request = Request.Builder()
            .url("https://arpan-fcm.herokuapp.com/send-notification-to-admin-app-about-a-new-order-that-he-recieved")
            .post(body)
            .build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(request: Request?, e: IOException?) {
                e!!.printStackTrace()
            }

            override fun onResponse(response: Response?) {
                Log.e("notifiication response" , response!!.message())
            }

        })
    }

    private fun placeOrder(
        name: String,
        details: String,
        image: Uri,
        userName: String,
        userNumber: String,
        userAddress: String,
        userNote: String
    ) {
        dialog.show()

        val key = "ORDER"+System.currentTimeMillis()

        val orderItemMain = OrderItemMain()
        orderItemMain.key = key
        orderItemMain.userId = FirebaseAuth.getInstance().currentUser!!.uid
        orderItemMain.userPhoneAccount = FirebaseAuth.getInstance().currentUser!!.phoneNumber!!
        orderItemMain.userName = userName
        orderItemMain.userNumber = userNumber
        orderItemMain.userNote = userNote
        orderItemMain.userAddress = userAddress
        orderItemMain.paymentMethod = if(viewMain.radioGroup.checkedRadioButtonId == R.id.rb1){
            "bKash"
        }else{
            "COD"
        }
        orderItemMain.totalPrice = 0
        orderItemMain.promoCodeApplied = promoCodeActive
        orderItemMain.promoCode = promoCode
        orderItemMain.locationItem = homeViewModel.getLocationArrayPickDrop()[viewMain.spinner_2.selectedItemPosition]
        orderItemMain.deliveryCharge = homeViewModel.getLocationArrayPickDrop()[viewMain.spinner_2.selectedItemPosition].deliveryCharge
        orderItemMain.daCharge = homeViewModel.getLocationArrayPickDrop()[viewMain.spinner_2.selectedItemPosition].daCharge
        if(promoCodeActive){
            if(homeViewModel.getLocationArrayPickDrop()[viewMain.spinner_2.selectedItemPosition].locationName.trim()=="মাগুরা সদর"){
                if(promoCode.discountPrice>homeViewModel.getLocationArrayPickDrop()[viewMain.spinner_2.selectedItemPosition].deliveryCharge){
                    orderItemMain.deliveryCharge = 0
                }else{
                    orderItemMain.deliveryCharge = homeViewModel.getLocationArrayPickDrop()[viewMain.spinner_2.selectedItemPosition].deliveryCharge - promoCode.discountPrice
                }
            }else{
                orderItemMain.deliveryCharge = homeViewModel.getLocationArrayPickDrop()[viewMain.spinner_2.selectedItemPosition].deliveryCharge
            }
        }
        orderItemMain.orderPlacingTimeStamp = System.currentTimeMillis()
        orderItemMain.lastTouchedTimeStamp = System.currentTimeMillis()

        val firebaseStorage = FirebaseStorage.getInstance()
            .reference.child("ORDER_IMAGES")
            .child(key)

        val cartProductEntity = ArrayList<CartProductEntity>()
        val cartProductEntity2 = CartProductEntity()
        cartProductEntity2.medicine_item = true
        cartProductEntity2.medicine_order_text = name
        cartProductEntity2.medicine_order_text_2 = details
        if(image.toString().isNotEmpty()){
            firebaseStorage.child("IMAGE_ORDER")
                .putFile(image).addOnCompleteListener {
                    cartProductEntity2.medicine_order_image = "IMAGE_ORDER"
                    cartProductEntity.add(cartProductEntity2)
                    orderItemMain.products = cartProductEntity
                    placeOrderFinalUpload(viewMain, orderItemMain)
                }
        }else{
            cartProductEntity2.medicine_order_image = ""
            cartProductEntity.add(cartProductEntity2)
            orderItemMain.products = cartProductEntity
            placeOrderFinalUpload(viewMain, orderItemMain)
        }

    }

    private fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)
        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar: Calendar = Calendar.getInstance()
        calendar.setTimeInMillis(milliSeconds)
        return formatter.format(calendar.getTime())
    }

}