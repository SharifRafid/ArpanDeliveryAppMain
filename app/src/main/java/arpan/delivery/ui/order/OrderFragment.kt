package arpan.delivery.ui.order

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import arpan.delivery.R
import arpan.delivery.data.adapters.OrderItemRecyclerAdapter
import arpan.delivery.data.adapters.OrderProductItemRecyclerAdapter
import arpan.delivery.data.db.CartProductEntity
import arpan.delivery.data.models.*
import arpan.delivery.ui.cart.CartViewModel
import arpan.delivery.ui.home.HomeActivity
import arpan.delivery.ui.home.HomeViewModel
import arpan.delivery.utils.Constants
import arpan.delivery.utils.createProgressDialog
import arpan.delivery.utils.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.shashank.sony.fancytoastlib.FancyToast
import com.squareup.okhttp.*
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.dialog_alert_layout_main.view.*
import kotlinx.android.synthetic.main.dialog_progress_layout_main.view.*
import kotlinx.android.synthetic.main.fragment_order.view.*
import mumayank.com.airlocationlibrary.AirLocation
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class OrderFragment : Fragment() {

    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var progressDialog : Dialog
    private lateinit var cartViewModel : CartViewModel
    private lateinit var homeViewModel : HomeViewModel
    private val mainCartCustomObjectHashMap = HashMap<String, ArrayList<CartProductEntity>>()
    private val mainShopItemHashMap = ArrayList<MainShopCartItem>()
    private lateinit var productRecyclerViewAdapter : OrderProductItemRecyclerAdapter
    private var currentCalc = 0
    private var deliveryCharges = ArrayList<Int>()
    private var deliveryLocations = ArrayList<String>()
    private var priceTotal = 0
    private var deliveryCharge = 0
    private var daCharge = 0

    private var singleShopMode = false
    private var promoCodeActive = false
    private var promoCode = PromoCode()
    private lateinit var dialogViewCustomAnimation : View
    private lateinit var dialog : Dialog
    private var uploadedItemCount = 0
    private lateinit var airLocation : AirLocation
    private var locationStatus = 0
    private var lat = ""
    private var lang = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_order, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (view.context as HomeActivity).titleActionBarTextView.text = getString(R.string.order_page_title)
        (view.context as HomeActivity).deleteItemsFromCart.visibility = View.GONE
        (view.context as HomeActivity).img_cart_icon.visibility = View.INVISIBLE
        initVars(view)
        initLogic(view)
    }

    private fun initLogic(view: View) {
        initSharedPreferencesForEdittexts(view)
        initCartProductsDataPlacement(view)
        initRadioGroup(view)
        initConfirmOrderClick(view)
    }

    private fun initConfirmOrderClick(view: View) {
        view.button.setOnClickListener {
            val userName = view.txt_name.text.toString()
            val userNumber = view.txt_number.text.toString()
            val userAddress = view.txt_address.text.toString()
            val userNote = view.txt_note.text.toString()
            if(userName.isNotEmpty()&&userNumber.isNotEmpty()&&userAddress.isNotEmpty()){
                if(cartViewModel.cartItems.value!!.isNotEmpty()){
                    val view2 = LayoutInflater.from(view.context)
                            .inflate(R.layout.dialog_alert_layout_main, null)
                    val dialog = AlertDialog.Builder(view.context)
                            .setView(view2).create()
                    view2.btnNoDialogAlertMain.text = getString(R.string.no)
                    view2.btnYesDialogAlertMain.text = getString(R.string.ok_text)
                    view2.titleTextView.text = getString(R.string.are_you_sure)
                    view2.messageTextView.text = getString(R.string.youre_finalizing_the_order)
                    view2.btnNoDialogAlertMain.setOnClickListener {
                        dialog.dismiss()
                    }
                    view2.btnYesDialogAlertMain.setOnClickListener {
                        dialog.dismiss()
                        placeOrderFinally(view, userName, userNumber, userAddress, userNote)
                    }
                    dialog.show()
                }else{
                    view.context.showToast(getString(R.string.you_have_no_products), FancyToast.ERROR)
                }
            }else{
                view.context.showToast(getString(R.string.no_products), FancyToast.ERROR)
            }
        }
    }

    private fun placeOrderFinally(view: View, userName: String, userNumber: String, userAddress: String, userNote: String) {
        dialogViewCustomAnimation = LayoutInflater.from(view.context).inflate(R.layout.dialog_progress_layout_main, null)
        dialog = AlertDialog.Builder(view.context)
                .setView(dialogViewCustomAnimation)
                .create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
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
        orderItemMain.products = cartViewModel.cartItems.value!!
        orderItemMain.promoCodeApplied  = promoCodeActive
        orderItemMain.promoCode = promoCode
        orderItemMain.lattitude = lat
        orderItemMain.longtitude = lang
        orderItemMain.paymentMethod = if(view.radioGroup.checkedRadioButtonId == R.id.rb1){
            "bKash"
        }else{
            "COD"
        }
        orderItemMain.totalPrice = priceTotal
        orderItemMain.locationItem = homeViewModel.getLocationArray()[view.spinner_1.selectedItemPosition]
        orderItemMain.deliveryCharge = deliveryCharge
        orderItemMain.daCharge = daCharge
        orderItemMain.orderPlacingTimeStamp = System.currentTimeMillis()
        orderItemMain.lastTouchedTimeStamp = System.currentTimeMillis()
        val firebaseStorage = FirebaseStorage.getInstance().reference.child("ORDER_IMAGES")
                .child(key)
        val images = ArrayList<OrderImageUploadItem>()
        for(item in cartViewModel.cartItems.value!!.filter {
            !it.product_item &&
                    it.parcel_order_image.isNotEmpty()||
                    it.medicine_order_image.isNotEmpty()||
                    it.custom_order_image.isNotEmpty()
        }){
            when {
                item.parcel_order_image.isNotEmpty() -> {
                    val imgKey = "OI"+item.id
                    images.add(
                            OrderImageUploadItem(
                                    name = imgKey,
                                    uri = item.parcel_order_image
                            )
                    )
                    orderItemMain.products[orderItemMain.products.indexOf(item)].parcel_order_image = imgKey
                }
                item.medicine_order_image.isNotEmpty() -> {
                    val imgKey = "OI"+item.id
                    images.add(
                            OrderImageUploadItem(
                                    name = imgKey,
                                    uri = item.medicine_order_image
                            )
                    )
                    orderItemMain.products[orderItemMain.products.indexOf(item)].medicine_order_image = imgKey
                }
                item.custom_order_image.isNotEmpty() -> {
                    val imgKey = "OI"+item.id
                    images.add(
                            OrderImageUploadItem(
                                    name = imgKey,
                                    uri = item.custom_order_image
                            )
                    )
                    orderItemMain.products[orderItemMain.products.indexOf(item)].custom_order_image = imgKey
                }
            }
        }
        if(images.isNotEmpty()){
            startUploadingImages(view, orderItemMain, firebaseStorage, images)
        }else{
            placeOrderFinalUpload(view, orderItemMain)
        }
    }

    private fun startUploadingImages(view: View, orderItemMain: OrderItemMain, firebaseStorage: StorageReference, images: ArrayList<OrderImageUploadItem>) {
        dialogViewCustomAnimation.animationView.setAnimation(R.raw.uploading)
        uploadedItemCount = 0
        uploadImage(view, orderItemMain, firebaseStorage, images)
    }

    private fun uploadImage(view: View, orderItemMain: OrderItemMain, firebaseStorage: StorageReference, images: ArrayList<OrderImageUploadItem>) {
        if(uploadedItemCount <= images.size-1){
            Log.e("IMAGE COUNT",images[uploadedItemCount].uri)
            firebaseStorage.child(images[uploadedItemCount].name)
                    .putFile(Uri.parse(images[uploadedItemCount].uri))
                    .addOnCompleteListener {
                        uploadedItemCount += 1
                        uploadImage(view, orderItemMain, firebaseStorage, images)
                    }
        }else{
            placeOrderFinalUpload(view, orderItemMain)
        }
    }

    private fun placeOrderFinalUpload(view: View, orderItemMain: OrderItemMain) {
        FirebaseDatabase.getInstance().reference.child("orderNumber")
                .child(getDate(System.currentTimeMillis(), "dd-MM-yyyy").toString())
                .child("ON")
                .get().addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        if(task.result!!.value==null){
                            orderItemMain.orderId = "ARP1001"
                            FirebaseDatabase.getInstance().reference.child("orderNumber")
                                    .child(getDate(System.currentTimeMillis(), "dd-MM-yyyy").toString())
                                    .child("ON").setValue("1001")
                        }else{
                            orderItemMain.orderId = "ARP"+(task.result!!.value.toString().toInt()+1)
                            FirebaseDatabase.getInstance().reference.child("orderNumber")
                                    .child(getDate(System.currentTimeMillis(), "dd-MM-yyyy").toString())
                                    .child("ON").setValue((task.result!!.value.toString().toInt()+1).toString())
                        }
                        if(promoCodeActive){
                            FirebaseDatabase.getInstance().reference
                                    .child("PROMO_CODES")
                                    .child(promoCode.key)
                                    .child("remainingUses")
                                    .setValue(promoCode.remainingUses-1)
                        }
                        dialogViewCustomAnimation.animationView.setAnimation(R.raw.uploading_completed)
                        FirebaseFirestore.getInstance().collection("users")
                                .document(FirebaseAuth.getInstance().currentUser!!.uid)
                                .collection("users_order_collection")
                                .add(orderItemMain)
                                .addOnCompleteListener {
                                    if(it.isSuccessful){
                                        view.context.showToast(getString(R.string.order_placed_successfully), FancyToast.SUCCESS)
                                        cartViewModel.deleteAll()
                                        (view.context as HomeActivity).navController.navigate(R.id.action_orderFragment_to_homeFragment)
                                        sendNotification(
                                            FirebaseAuth.getInstance().currentUser!!.uid,
                                            "নতুন অর্ডার ${orderItemMain.orderId}",
                                            "আপনি একটি নতুন অর্ডার পেয়েছেন দ্রুত অর্ডার টি কনফার্ম করুন। ধন্যবাদ ।",
                                            it.result!!.id
                                        )
                                        dialog.dismiss()
                                    }else{
                                        dialog.dismiss()
                                        it.exception!!.printStackTrace()
                                        view.context.showToast(getString(R.string.failed_order), FancyToast.ERROR)
                                    }
                                }
                    }else{
                        dialog.dismiss()
                        task.exception!!.printStackTrace()
                        view.context.showToast(getString(R.string.failed_order), FancyToast.ERROR)
                    }
                }
    }

    fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)
        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar: Calendar = Calendar.getInstance()
        calendar.setTimeInMillis(milliSeconds)
        return formatter.format(calendar.getTime())
    }

    private fun initRadioGroup(view: View) {
        view.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            if(checkedId == R.id.rb1){
                view.bkash_charge_note.visibility = View.VISIBLE
            }else{
                view.bkash_charge_note.visibility = View.GONE
            }
        }
    }

    private fun initCartProductsDataPlacement(view: View) {
        workWithTheArrayList(cartViewModel.cartItems.value!!, view)
        initSpinnerLocations(view)
    }

    private fun workWithTheArrayList(list: List<CartProductEntity>, view: View) {
        for(cartProductEntity in list){
            when {
                cartProductEntity.parcel_item -> {
                    mainCartCustomObjectHashMap["parcel_item"]?.add(cartProductEntity)
                }
                cartProductEntity.custom_order_item -> {
                    mainCartCustomObjectHashMap["custom_order_item"]?.add(cartProductEntity)
                }
                cartProductEntity.medicine_item -> {
                    mainCartCustomObjectHashMap["medicine_item"]?.add(cartProductEntity)
                }
                else -> {
                    mainCartCustomObjectHashMap["product_item"]?.add(cartProductEntity)
                }
            }
        }
        if(mainCartCustomObjectHashMap["product_item"]!!.isNotEmpty()){
            view.productsTextView.visibility = View.VISIBLE
            view.productsRecyclerView.visibility = View.VISIBLE
            view.applyPromoCodeLinear.visibility = View.VISIBLE
            initiateRestLogicForArrayList(view)
            initiatePromoCodeLogic(view)
        }else{
            view.applyPromoCodeLinear.visibility = View.GONE
            view.productsTextView.visibility = View.GONE
            view.productsRecyclerView.visibility = View.GONE
        }
        if(mainCartCustomObjectHashMap["parcel_item"]!!.isNotEmpty()){
            view.parcelOrderTextView.visibility = View.VISIBLE
            //view.parcelOrderTextView2.visibility = View.VISIBLE
            view.parcelRecyclerView.visibility = View.VISIBLE
            initiateRestLogicForParcel(view)
        }else{
            view.parcelOrderTextView.visibility = View.GONE
            view.parcelOrderTextView2.visibility = View.GONE
            view.parcelRecyclerView.visibility = View.GONE
        }
        if(mainCartCustomObjectHashMap["custom_order_item"]!!.isNotEmpty()){
            view.customOrderTextView.visibility = View.VISIBLE
            //view.customOrderTextView2.visibility = View.VISIBLE
            view.customOrderRecyclerView.visibility = View.VISIBLE
            initiateRestLogicForCustomOrder(view)
        }else{
            view.customOrderTextView.visibility = View.GONE
            view.customOrderTextView2.visibility = View.GONE
            view.customOrderRecyclerView.visibility = View.GONE
        }
        if(mainCartCustomObjectHashMap["medicine_item"]!!.isNotEmpty()){
            view.medicineOrderTextView.visibility = View.VISIBLE
            //view.medicineOrderTextView2.visibility = View.VISIBLE
            view.medicineRecyclerView.visibility = View.VISIBLE
            initiateRestLogicForMedicine(view)
        }else{
            view.medicineOrderTextView.visibility = View.GONE
            view.medicineOrderTextView2.visibility = View.GONE
            view.medicineRecyclerView.visibility = View.GONE
        }
    }

    private fun initSpinnerLocations(view: View) {
        val adapter = ArrayAdapter(
                view.context,
                R.layout.custom_spinner_view,
                deliveryLocations
        )
        adapter.setDropDownViewResource(R.layout.custom_spinner_item_view)
        view.spinner_1.adapter = adapter
        view.spinner_1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view2: View?,
                    position: Int,
                    id: Long
            ) {
                setPriceTotalOnView(view)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                view.spinner_1.setSelection(0)
            }
        }
    }

    private fun initiateRestLogicForMedicine(view: View) {
        val cartItemRecyclerAdapter = mainCartCustomObjectHashMap["medicine_item"]?.let { OrderItemRecyclerAdapter(view.context, it) }
        view.medicineRecyclerView.layoutManager = LinearLayoutManager(view.context)
        view.medicineRecyclerView.adapter = cartItemRecyclerAdapter
    }

    private fun initiateRestLogicForCustomOrder(view: View) {
        val cartItemRecyclerAdapter = mainCartCustomObjectHashMap["custom_order_item"]?.let { OrderItemRecyclerAdapter(view.context, it) }
        view.customOrderRecyclerView.layoutManager = LinearLayoutManager(view.context)
        view.customOrderRecyclerView.adapter = cartItemRecyclerAdapter
    }

    private fun initiateRestLogicForParcel(view: View) {
        val cartItemRecyclerAdapter = mainCartCustomObjectHashMap["parcel_item"]?.let { OrderItemRecyclerAdapter(view.context, it) }
        view.parcelRecyclerView.layoutManager = LinearLayoutManager(view.context)
        view.parcelRecyclerView.adapter = cartItemRecyclerAdapter
    }

    private fun initiateRestLogicForArrayList(view: View) {
        mainShopItemHashMap.clear()
        for(cartItemEntity in mainCartCustomObjectHashMap["product_item"]!!){
            val filteredArray = mainShopItemHashMap.filter { it -> it.shop_doc_id == cartItemEntity.product_item_shop_key }
            if(filteredArray.isEmpty()){
                val shopItem = MainShopCartItem()
                shopItem.shop_doc_id = cartItemEntity.product_item_shop_key
                shopItem.cart_products.add(cartItemEntity)
                mainShopItemHashMap.add(shopItem)
            }else{
                mainShopItemHashMap[mainShopItemHashMap.indexOf(filteredArray[0])]
                    .cart_products.add(cartItemEntity)
            }
        }
        if(mainShopItemHashMap.isNotEmpty()){
            progressDialog.show()
            currentCalc = 0
            fillUpShopDetailsValueInMainShopItemList(view)
        }
    }

    private fun fillUpShopDetailsValueInMainShopItemList(view: View) {
        firebaseFirestore.collection(Constants.FC_SHOPS_MAIN)
            .document(mainShopItemHashMap[currentCalc].shop_doc_id)
            .get().addOnSuccessListener { document ->
                mainShopItemHashMap[currentCalc].shop_details =
                    ShopItem(
                            key = document.id,
                            name = document.getString(Constants.FIELD_FD_SM_NAME).toString(),
                            categories = document.getString(Constants.FIELD_FD_SM_CATEGORY).toString(),
                            image = document.getString(Constants.FIELD_FD_SM_ICON).toString(),
                            cover_image = document.getString(Constants.FIELD_FD_SM_COVER).toString(),
                            da_charge = document.getString(Constants.FIELD_FD_SM_DA_CHARGE).toString(),
                            deliver_charge = document.getString(Constants.FIELD_FD_SM_DELIVERY).toString(),
                            location = document.getString(Constants.FIELD_FD_SM_LOCATION).toString(),
                            username = document.getString(Constants.FIELD_FD_SM_USERNAME).toString(),
                            password = document.getString(Constants.FIELD_FD_SM_PASSWORD).toString(),
                            order = document.getString(Constants.FIELD_FD_SM_ORDER).toString().toInt()
                    )
                if(currentCalc+1 >= mainShopItemHashMap.size){
                    // The data is downloaded all of those
                    view.productsRecyclerView.layoutManager = LinearLayoutManager(view.context)
                    view.productsRecyclerView.adapter = productRecyclerViewAdapter
                    calculateTotalPrice(view)
                    progressDialog.dismiss()
                    initLocationDetectingProcessWithUsersPermissionAsWell(view)
                }else{
                    currentCalc ++
                    fillUpShopDetailsValueInMainShopItemList(view)
                }
            }
    }

    private fun initLocationDetectingProcessWithUsersPermissionAsWell(view: View) {
//        val dialogView = LayoutInflater.from(view.context)
//                .inflate(R.layout.dialog_alert_layout_main, null)
//        val dialog = AlertDialog.Builder(view.context)
//                .setView(dialogView).create()
//        dialogView.btnNoDialogAlertMain.text = getString(R.string.no)
//        dialogView.btnYesDialogAlertMain.text = getString(R.string.ok_text)
//        dialogView.titleTextView.text = getString(R.string.location_permission)
//        dialogView.messageTextView.text = getString(R.string.do_you_want_to_track_location)
//        dialogView.btnNoDialogAlertMain.setOnClickListener {
//            view.location.visibility = View.GONE
//            dialog.dismiss()
//        }
//        dialogView.btnYesDialogAlertMain.setOnClickListener {view.location.visibility = View.VISIBLE
//            airLocation = AirLocation(view.context as HomeActivity, object : AirLocation.Callback {
//                override fun onSuccess(locations: ArrayList<Location>) {
//                    locationStatus = 1
//                    lat = locations[0].latitude.toString()
//                    lang = locations[0].longitude.toString()
//                    Log.e("LOCATION", "$lat,$lang")
//                    view.location.text = "আপনার লোকেশন ডিটেক্ট করা গিয়েছে। ধন্যবাদ"
//                }
//                override fun onFailure(locationFailedEnum: AirLocation.LocationFailedEnum) {
//                    locationStatus = 2
//                    //view.location.text = "আপনার লোকেশন ডিটেক্ট করা যায়নি আবার চেষ্টা করতে ক্লিক করুন"
//                    view.location.text = "দ্রুততম ডেলিভারির জন্য আপনার লোকেশন ডিটেক্ট করা হচ্ছে......"
//                    airLocation.start()
//                }
//            },true)
//            airLocation.start()
////                view.location.setOnClickListener {
////                    view.location.text = "দ্রুততম ডেলিভারির জন্য আপনার লোকেশন ডিটেক্ট করা হচ্ছে......"
////                    airLocation.start()
////                }
//            dialog.dismiss()
//        }
//        dialog.show()
        view.location.visibility = View.VISIBLE
        view.location.text = "দ্রুততম ডেলিভারির জন্য আপনার লোকেশন ডিটেক্ট করা হচ্ছে......"
        airLocation = AirLocation(view.context as HomeActivity, object : AirLocation.Callback {
            override fun onSuccess(locations: ArrayList<Location>) {
                locationStatus = 1
                lat = locations[0].latitude.toString()
                lang = locations[0].longitude.toString()
                Log.e("LOCATION", "$lat,$lang")
                view.location.text = "আপনার লোকেশন ডিটেক্ট করা গিয়েছে। ধন্যবাদ\nলোকেশন দিতে না চাইলে এখানে ট্যাপ করুন"
                view.location.setOnClickListener {
                    val dialogView = LayoutInflater.from(view.context)
                            .inflate(R.layout.dialog_alert_layout_main, null)
                    val dialog = AlertDialog.Builder(view.context)
                            .setView(dialogView).create()
                    dialogView.btnNoDialogAlertMain.text = getString(R.string.no)
                    dialogView.btnYesDialogAlertMain.text = getString(R.string.ok_text)
                    dialogView.titleTextView.text = getString(R.string.location_permission)
                    dialogView.messageTextView.text = getString(R.string.do_you_want_to_track_location)
                    dialogView.btnNoDialogAlertMain.setOnClickListener {
                        view.location.visibility = View.GONE
                        lat = "0"
                        lang = "0"
                        dialog.dismiss()
                    }
                    dialogView.btnYesDialogAlertMain.setOnClickListener {view.location.visibility = View.VISIBLE
                        dialog.dismiss()
                    }
                    dialog.show()
                }
            }
            override fun onFailure(locationFailedEnum: AirLocation.LocationFailedEnum) {
                locationStatus = 2
                //view.location.text = "আপনার লোকেশন ডিটেক্ট করা যায়নি আবার চেষ্টা করতে ক্লিক করুন"
                initLocationDetectingProcessWithUsersPermissionAsWell(view)
            }
        },true)
        airLocation.start()
    }

    private fun calculateTotalPrice(view: View) {
        if(mainShopItemHashMap.isEmpty()){
            // No Product Item
        }else{
            if((view.context as HomeActivity).cartItemsAllMainList.any { !it.product_item }){
                singleShopMode = false
                priceTotal = 0
                deliveryCharge = deliveryCharges[view.spinner_1.selectedItemPosition]
                daCharge = (view.context as HomeActivity).homeViewModel
                    .getLocationArray()[view.spinner_1.selectedItemPosition].daCharge
                for(shop in mainShopItemHashMap){
                    for (pdct in shop.cart_products){
                        priceTotal += (pdct.product_item_price * pdct.product_item_amount)
                    }
                }
                setPriceTotalOnView(view)
            }else{
                if(mainShopItemHashMap.size == 1){
                    singleShopMode = true
                    priceTotal = 0
                    deliveryCharge = mainShopItemHashMap[0].shop_details.deliver_charge.toInt()
                    daCharge = mainShopItemHashMap[0].shop_details.da_charge.toInt()
                    for (pdct in mainShopItemHashMap[0].cart_products){
                        priceTotal += (pdct.product_item_price * pdct.product_item_amount)
                    }
                    setPriceTotalOnView(view)
                }else{
                    singleShopMode = false
                    priceTotal = 0
                    deliveryCharge = deliveryCharges[view.spinner_1.selectedItemPosition]
                    daCharge = (view.context as HomeActivity).homeViewModel
                        .getLocationArray()[view.spinner_1.selectedItemPosition].daCharge
                    for(shop in mainShopItemHashMap){
                        for (pdct in shop.cart_products){
                            priceTotal += (pdct.product_item_price * pdct.product_item_amount)
                        }
                    }
                    setPriceTotalOnView(view)
                }
            }
        }
    }

    private fun setPriceTotalOnView(view: View) {
        val position = view.spinner_1.selectedItemPosition
        if(position==0){
            view.text_address_container.visibility = View.VISIBLE
            if(singleShopMode){
                deliveryCharge = mainShopItemHashMap[0].shop_details.deliver_charge.toInt()
                daCharge = mainShopItemHashMap[0].shop_details.da_charge.toInt()
            }else{
                if(mainShopItemHashMap.size <= homeViewModel.getMaxShops().value!!){
                    deliveryCharge = deliveryCharges[view.spinner_1.selectedItemPosition]
                    daCharge = (view.context as HomeActivity).homeViewModel
                        .getLocationArray()[view.spinner_1.selectedItemPosition].daCharge
                }else{
                    deliveryCharge = deliveryCharges[view.spinner_1.selectedItemPosition] +
                            ((mainShopItemHashMap.size-
                                    homeViewModel.getMaxShops().value!!)*
                                    homeViewModel.getDeliveryChargeExtra().value!!)
                    daCharge = (view.context as HomeActivity).homeViewModel
                        .getLocationArray()[view.spinner_1.selectedItemPosition].daCharge +
                            ((mainShopItemHashMap.size-
                                    homeViewModel.getMaxShops().value!!)*
                                    homeViewModel.getDAChargeExtra().value!!)
                }
            }
        }else{
            deliveryCharge = deliveryCharges[view.spinner_1.selectedItemPosition]
            daCharge = (view.context as HomeActivity).homeViewModel
                .getLocationArray()[view.spinner_1.selectedItemPosition].daCharge
            view.text_address_container.visibility = View.GONE
        }
        view.txtAllPrice.text = getString(R.string.total_total_text)+" "+"${priceTotal}+${deliveryCharge} " +
                "= ${priceTotal+deliveryCharge} "+getString(R.string.taka_text)
        if(promoCodeActive){
            if(priceTotal <= promoCode.discountPrice){
                view.txtAllPrice.text = getString(R.string.total_total_text)+" "+"${0}+${deliveryCharge} " +
                        "= ${0+deliveryCharge} "+getString(R.string.taka_text)
            }else{
                view.txtAllPrice.text = getString(R.string.total_total_text)+" "+
                        "${priceTotal - promoCode.discountPrice}+${deliveryCharge} " +
                        "= ${priceTotal - promoCode.discountPrice+deliveryCharge} "+
                        getString(R.string.taka_text)
            }
        }
    }

    private fun initiatePromoCodeLogic(view: View) {
        view.addPromoCode.setOnClickListener {
            view.applyPromoCodeLinear.visibility = View.GONE
            view.promoCodeLinear.visibility = View.VISIBLE
            view.promoCodeAppliedLinear.visibility = View.GONE
        }
        view.apply_promo_code.setOnClickListener {
            if(view.edt_coupon_code.text.isNotEmpty()){
                progressDialog.show()
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
                                        promoCodesArray.filter { promoCode ->
                                            if(promoCode.promoCodeName == view.edt_coupon_code.text.toString()){
                                                if(promoCode.minimumPrice <= priceTotal){
                                                    if(promoCode.active){
                                                        if(promoCode.remainingUses!=0){
                                                            if(promoCode.validityOfCode>System.currentTimeMillis()){
                                                                true
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
                                                    errorMessage = getString(R.string.minimum_order_value_text) + " " +
                                                            promoCode.minimumPrice +" " + getString(R.string.taka)
                                                    false
                                                }
                                            }else{
                                                false
                                            }
                                        }
                                if(filteredArray.isEmpty()){
                                    progressDialog.dismiss()
                                    view.context.showToast(errorMessage, FancyToast.ERROR)
                                }else{
                                    promoCodeActive = true
                                    promoCode = filteredArray[0]
                                    view.promoCodeLinear.visibility = View.GONE
                                    view.promoCodeAppliedLinear.visibility =  View.VISIBLE
                                    view.applyPromoCodeLinear.visibility = View.GONE
                                    view.promoCodeAppliedText.text = getString(R.string.your_promo_code_is_applied_1)+
                                            " "+promoCode.discountPrice+" "+getString(R.string.your_promo_code_is_applied_2)
                                    view.promoCodeAppliedRemoveText.setOnClickListener {
                                        promoCodeActive = false
                                        promoCode = PromoCode()
                                        view.applyPromoCodeLinear.visibility = View.VISIBLE
                                        view.promoCodeLinear.visibility = View.GONE
                                        view.promoCodeAppliedLinear.visibility = View.GONE
                                        view.edt_coupon_code.setText("")
                                        calculateTotalPrice(view)
                                    }
                                    setPriceTotalOnView(view)
                                    progressDialog.dismiss()
                                }
                            }else{
                                it.exception!!.printStackTrace()
                            }
                        }
            }
        }
    }

    private fun initSharedPreferencesForEdittexts(view: View) {
        view.txt_name.setText((view.context as HomeActivity).userNameFromProfile)
        view.txt_number.setText(firebaseAuth.currentUser!!.phoneNumber!!.toString())
        view.txt_address.setText((view.context as HomeActivity).userAddressFromProfile)
    }

    private fun initVars(view: View) {
        firebaseFirestore = FirebaseFirestore.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = view.context.createProgressDialog()
        cartViewModel = (view.context as HomeActivity).cartViewModel
        homeViewModel = activity?.let { ViewModelProvider(it).get(HomeViewModel::class.java) }!!
        productRecyclerViewAdapter =
            OrderProductItemRecyclerAdapter(view.context, mainShopItemHashMap)
        for(item in homeViewModel.getLocationArray()) {
            deliveryCharges.add(item.deliveryCharge)
            deliveryLocations.add(item.locationName)
        }
        mainCartCustomObjectHashMap["product_item"] = ArrayList()
        mainCartCustomObjectHashMap["parcel_item"] = ArrayList()
        mainCartCustomObjectHashMap["custom_order_item"] = ArrayList()
        mainCartCustomObjectHashMap["medicine_item"] = ArrayList()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        airLocation.onActivityResult(requestCode, resultCode, data) // ADD THIS LINE INSIDE onActivityResult
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        airLocation.onRequestPermissionsResult(requestCode, permissions, grantResults) // ADD THIS LINE INSIDE onRequestPermissionResult
    }
}