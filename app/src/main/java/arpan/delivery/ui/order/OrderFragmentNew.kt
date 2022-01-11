package arpan.delivery.ui.order

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import arpan.delivery.R
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
import com.google.gson.Gson
import com.shashank.sony.fancytoastlib.FancyToast
import com.squareup.okhttp.*
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.dialog_alert_layout_main.view.*
import kotlinx.android.synthetic.main.dialog_progress_layout_main.view.*
import kotlinx.android.synthetic.main.fragment_order_new.*
import kotlinx.android.synthetic.main.fragment_order_new.view.*
import mumayank.com.airlocationlibrary.AirLocation
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt


class OrderFragmentNew : Fragment() {
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var progressDialog : Dialog
    private lateinit var cartViewModel : CartViewModel
    private lateinit var homeViewModel : HomeViewModel
    private val mainCartCustomObjectHashMap = ArrayList<CartProductEntity>()
    private val mainShopItemHashMap = ArrayList<MainShopCartItem>()
    private lateinit var productRecyclerViewAdapter : OrderProductItemRecyclerAdapter
    private var currentCalc = 0
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

    // DO NOT CHANGE BECAUSE IT HAS REALTIME CALCULATIONS CONNECTED TO IT
    private val BKASH_CHARGE_PERCENTAGE = 0.0185f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_order_new, container, false)
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
            if(userName.isNotEmpty()&&userNumber.isNotEmpty()){
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
                view.context.showToast("আপনার নাম এবং নাম্বার দিন ।", FancyToast.ERROR)
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
        orderItemMain.promoCodeApplied = promoCodeActive
        orderItemMain.totalPrice = priceTotal
        orderItemMain.daCharge = daCharge
        orderItemMain.deliveryCharge = deliveryCharge
        if(promoCodeActive){
            when {
                promoCode.shopDiscount -> {
                    if(priceTotal<promoCode.discountPrice){
                        orderItemMain.totalPrice = 0
                    }else{
                        orderItemMain.totalPrice = priceTotal-promoCode.discountPrice
                    }
                    Log.e("PRICE1",orderItemMain.totalPrice.toString())
                }
//                promoCode.bothDiscount -> {
//                    if(priceTotal+deliveryCharge<promoCode.discountPrice){
//                        orderItemMain.totalPrice = 0
//                        orderItemMain.deliveryCharge = 0
//                    }else{
//                        if(promoCode.discountPrice>priceTotal){
//                            orderItemMain.totalPrice = 0
//                            if(promoCode.discountPrice-priceTotal>deliveryCharge){
//                                orderItemMain.deliveryCharge = 0
//                            }else{
//                                orderItemMain.deliveryCharge -= promoCode.discountPrice
//                            }
//                        }else{
//                            orderItemMain.totalPrice -= promoCode.discountPrice
//                        }
//                    }
//                    Log.e("PRICE2",orderItemMain.totalPrice.toString())
//                    Log.e("PRICE2",orderItemMain.deliveryCharge.toString())
//                }
                promoCode.deliveryDiscount -> {
                    if(deliveryCharge<promoCode.discountPrice){
                        orderItemMain.deliveryCharge = 0
                    }else{
                        orderItemMain.deliveryCharge-=promoCode.discountPrice
                    }
                    Log.e("PRICE3",orderItemMain.deliveryCharge.toString())
                }
            }
        }
        if(radioGroup.checkedRadioButtonId == R.id.rb1){
            orderItemMain.totalPrice += roundNumberPriceTotal((orderItemMain.totalPrice+orderItemMain.deliveryCharge)
                    *BKASH_CHARGE_PERCENTAGE).toInt()
        }
        orderItemMain.promoCode = promoCode
        orderItemMain.lattitude = lat
        orderItemMain.longtitude = lang
        orderItemMain.paymentMethod = if(view.radioGroup.checkedRadioButtonId == R.id.rb1){
            "bKash"
        }else{
            "COD"
        }
        orderItemMain.locationItem = homeViewModel.getLocationArray()[view.spinner_1.selectedItemPosition]
        orderItemMain.orderPlacingTimeStamp = System.currentTimeMillis()
        orderItemMain.lastTouchedTimeStamp = System.currentTimeMillis()

        placeOrderFinalUpload(view, orderItemMain)
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
                                    .child("ON")
                                    .setValue((task.result!!.value.toString().toInt()+1).toString())
                        }
                        if(promoCodeActive){
                            FirebaseDatabase.getInstance().reference
                                    .child("PROMO_CODES")
                                    .child(promoCode.key).get().addOnCompleteListener { it2 ->
                                    if(it2.isSuccessful){
                                            val newPromoCode = it2.result.getValue(PromoCode::class.java)!!
                                            newPromoCode.remainingUses -= 1
                                            if(newPromoCode.onceForOneUser){
                                                newPromoCode.userIds += (","+firebaseAuth.currentUser!!.uid+",")
                                            }
                                            FirebaseDatabase.getInstance().reference
                                                .child("PROMO_CODES")
                                                .child(promoCode.key)
                                                .setValue(newPromoCode)
                                        }
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(FirebaseAuth.getInstance().currentUser!!.uid)
                                        .collection("users_order_collection")
                                        .add(orderItemMain)
                                        .addOnCompleteListener {
                                            if(it.isSuccessful){
                                                view.context.showToast(getString(R.string.order_placed_successfully), FancyToast.SUCCESS)
                                                cartViewModel.deleteAll()
                                                val bundle = Bundle()
                                                bundle.putString("orderID",it.result.id)
                                                (view.context as HomeActivity).navController.navigate(R.id.action_homeFragment_self, bundle)
                                                (view.context as HomeActivity).navController.navigate(R.id.orderHistoryFragment, bundle)
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
                                }
                        }else{
                            FirebaseFirestore.getInstance().collection("users")
                                .document(FirebaseAuth.getInstance().currentUser!!.uid)
                                .collection("users_order_collection")
                                .add(orderItemMain)
                                .addOnCompleteListener {
                                    if(it.isSuccessful){
                                        view.context.showToast(getString(R.string.order_placed_successfully), FancyToast.SUCCESS)
                                        cartViewModel.deleteAll()
                                        val bundle = Bundle()
                                        bundle.putString("orderID",it.result.id)
                                        (view.context as HomeActivity).navController.navigate(R.id.action_homeFragment_self, bundle)
                                        (view.context as HomeActivity).navController.navigate(R.id.orderHistoryFragment, bundle)
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
            setPriceTotalOnView(view)
        }
    }

    private fun initCartProductsDataPlacement(view: View) {
        workWithTheArrayList(cartViewModel.cartItems.value!!, view)
        initSpinnerLocations(view)
    }

    private fun workWithTheArrayList(list: List<CartProductEntity>, view: View) {
        mainCartCustomObjectHashMap.clear()
        for(cartProductEntity in list){
            if(cartProductEntity.product_item){
                mainCartCustomObjectHashMap.add(cartProductEntity)
            }
        }
        if(mainCartCustomObjectHashMap.isNotEmpty()){
            view.productsRecyclerView.visibility = View.VISIBLE
            view.applyPromoCodeLinear.visibility = View.VISIBLE
            initiateRestLogicForArrayList(view)
            initiatePromoCodeLogic(view)
        }else{
            view.applyPromoCodeLinear.visibility = View.GONE
            view.productsRecyclerView.visibility = View.GONE
        }
    }

    private fun initSpinnerLocations(view: View) {
        deliveryLocations.clear()
        homeViewModel.getLocationArray().forEach {
            deliveryLocations.add(it.locationName)
        }
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
                if(deliveryLocations[position].trim()=="মাগুরা সদর"){
                    view.text_address_container.visibility = View.VISIBLE
                }else{
                    view.text_address_container.visibility = View.GONE
                }
                setPriceTotalOnView(view)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                view.spinner_1.setSelection(0)
            }
        }
    }

    private fun setPriceTotalOnView(view: View) {
        val locationItem = homeViewModel.getLocationArray()[view.spinner_1.selectedItemPosition]
        if(mainShopItemHashMap.size == 1){
            if(locationItem.locationName.trim() == "মাগুরা সদর"){
                deliveryCharge = mainShopItemHashMap[0].shop_details.deliver_charge.toInt()
                daCharge = mainShopItemHashMap[0].shop_details.da_charge.toInt()
            }else{
                if(mainShopItemHashMap[0].shop_details.isClient == "yes"){
                    deliveryCharge = locationItem.deliveryChargeClient
                    daCharge = locationItem.daCharge
                }else{
                    deliveryCharge = locationItem.deliveryCharge
                    daCharge = locationItem.daCharge
                }
            }
        }else{
            if(mainShopItemHashMap.size <= homeViewModel.getMaxShops().value!!){
                deliveryCharge = (view.context as HomeActivity).homeViewModel
                    .getLocationArray()[view.spinner_1.selectedItemPosition].deliveryCharge
                daCharge = (view.context as HomeActivity).homeViewModel
                    .getLocationArray()[view.spinner_1.selectedItemPosition].daCharge
            }else{
                deliveryCharge = (view.context as HomeActivity).homeViewModel
                    .getLocationArray()[view.spinner_1.selectedItemPosition].deliveryCharge +
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
        if(promoCodeActive){
            when {
                promoCode.shopDiscount -> {
                    if(priceTotal<promoCode.discountPrice){
                        if(radioGroup.checkedRadioButtonId == R.id.rb1){
                            val ptdvbc = roundNumberPriceTotal((0+deliveryCharge)*BKASH_CHARGE_PERCENTAGE)
                            view.bkash_charge_note.text = "বিকাশ চার্জ হিসাবে ${ptdvbc} টাকা যোগ করা হয়েছে"

                            view.txtAllPrice.text = "মোটঃ ${0+ptdvbc} + ${deliveryCharge} = ${0+deliveryCharge+ptdvbc} টাকা"
                        }else{
                            view.txtAllPrice.text = "মোটঃ ${0} + ${deliveryCharge} = ${0+deliveryCharge} টাকা"
                        }
                    }else{
                        if(radioGroup.checkedRadioButtonId == R.id.rb1){
                            val ptdvbc = roundNumberPriceTotal((priceTotal-promoCode.discountPrice+deliveryCharge)*BKASH_CHARGE_PERCENTAGE)
                            view.bkash_charge_note.text = "বিকাশ চার্জ হিসাবে ${ptdvbc} টাকা যোগ করা হয়েছে"
                            view.txtAllPrice.text = "মোটঃ ${priceTotal-promoCode.discountPrice+ptdvbc} + ${deliveryCharge} = ${priceTotal-promoCode.discountPrice+deliveryCharge+ptdvbc} টাকা"
                        }else{
                            view.txtAllPrice.text = "মোটঃ ${priceTotal-promoCode.discountPrice} + ${deliveryCharge} = ${priceTotal-promoCode.discountPrice+deliveryCharge} টাকা"
                        }
                    }
                }
//
//                promoCode.bothDiscount -> {
//                    if(priceTotal+deliveryCharge<promoCode.discountPrice){
//                        if(radioGroup.checkedRadioButtonId == R.id.rb1){
//                            val ptdvbc = 0
//                            view.bkash_charge_note.text = "বিকাশ চার্জ হিসাবে ${ptdvbc} টাকা যোগ করা হয়েছে"
//                            view.txtAllPrice.text = "মোটঃ ${priceTotal+ptdvbc} + ${deliveryCharge} = ${0+ptdvbc} টাকা"
//                        }else{
//                            view.txtAllPrice.text = "মোটঃ ${priceTotal} + ${deliveryCharge} = ${0} টাকা"
//                        }
//                    }else{
//                        if(radioGroup.checkedRadioButtonId == R.id.rb1){
//                            val ptdvbc = roundNumberPriceTotal((priceTotal+deliveryCharge-promoCode.discountPrice)*BKASH_CHARGE_PERCENTAGE)
//                            view.bkash_charge_note.text = "বিকাশ চার্জ হিসাবে ${ptdvbc} টাকা যোগ করা হয়েছে"
//                            view.txtAllPrice.text = "মোটঃ ${priceTotal+ptdvbc} + ${deliveryCharge} = ${priceTotal+deliveryCharge-promoCode.discountPrice+ptdvbc} টাকা"
//                        }else{
//                            view.txtAllPrice.text = "মোটঃ ${priceTotal} + ${deliveryCharge} = ${priceTotal+deliveryCharge-promoCode.discountPrice} টাকা"
//                        }
//                    }
//                }
                promoCode.deliveryDiscount -> {
                    if(locationItem.locationName.trim() == "মাগুরা সদর"){
                        if(deliveryCharge<promoCode.discountPrice){
                            if(radioGroup.checkedRadioButtonId == R.id.rb1){
                                val ptdvbc = roundNumberPriceTotal((priceTotal+0)*BKASH_CHARGE_PERCENTAGE)
                                view.bkash_charge_note.text = "বিকাশ চার্জ হিসাবে ${ptdvbc} টাকা যোগ করা হয়েছে"
                                view.txtAllPrice.text = "মোটঃ ${priceTotal+ptdvbc} + ${0} = ${priceTotal+0+ptdvbc} টাকা"
                            }else{
                                view.txtAllPrice.text = "মোটঃ ${priceTotal} + ${0} = ${priceTotal+0} টাকা"
                            }
                        }else{
                            if(radioGroup.checkedRadioButtonId == R.id.rb1){
                                val ptdvbc = roundNumberPriceTotal((priceTotal+deliveryCharge-promoCode.discountPrice)*BKASH_CHARGE_PERCENTAGE)
                                view.bkash_charge_note.text = "বিকাশ চার্জ হিসাবে ${ptdvbc} টাকা যোগ করা হয়েছে"
                                view.txtAllPrice.text = "মোটঃ ${priceTotal+ptdvbc} + ${deliveryCharge-promoCode.discountPrice} = ${priceTotal+deliveryCharge-promoCode.discountPrice+ptdvbc} টাকা"
                            }else{
                                view.txtAllPrice.text = "মোটঃ ${priceTotal} + ${deliveryCharge-promoCode.discountPrice} = ${priceTotal+deliveryCharge-promoCode.discountPrice} টাকা"
                            }
                        }
                    }else{
                        view.context.showToast("প্রোমো কোডটি শুধু মাত্র মাগুরা সদর এর জন্য প্রযোজ্য হবে।", FancyToast.ERROR)
                        if(radioGroup.checkedRadioButtonId == R.id.rb1){
                            val ptdvbc = roundNumberPriceTotal((priceTotal+deliveryCharge)*BKASH_CHARGE_PERCENTAGE)
                            view.txtAllPrice.text = "মোটঃ ${priceTotal+ptdvbc} +" +
                                    " ${deliveryCharge} = ${priceTotal+deliveryCharge+ptdvbc} টাকা"
                            view.bkash_charge_note.text = "বিকাশ চার্জ হিসাবে ${ptdvbc} টাকা যোগ করা হয়েছে"
                        }else{
                            view.txtAllPrice.text = "মোটঃ ${priceTotal} + ${deliveryCharge} = ${priceTotal+deliveryCharge} টাকা"
                        }
                    }
                }
            }
        }else{
            if(radioGroup.checkedRadioButtonId == R.id.rb1){
                val ptdvbc = roundNumberPriceTotal((priceTotal+deliveryCharge)*BKASH_CHARGE_PERCENTAGE)
                view.txtAllPrice.text = "মোটঃ ${priceTotal+ptdvbc} +" +
                        " ${deliveryCharge} = ${priceTotal+deliveryCharge+ptdvbc} টাকা"
                view.bkash_charge_note.text = "বিকাশ চার্জ হিসাবে ${ptdvbc} টাকা যোগ করা হয়েছে"
            }else{
                view.txtAllPrice.text = "মোটঃ ${priceTotal} + ${deliveryCharge} = ${priceTotal+deliveryCharge} টাকা"
            }
        }
    }

    private fun roundNumberPriceTotal(d: Float): Int {
        //This  is a special round function exclusively for this  page of the app
        //not usable for general parts and other parts of   the code or apps
        return if(d > d.toInt()){
            d.toInt()+1
        }else{
            d.roundToInt()
        }
    }

    private fun initiateRestLogicForArrayList(view: View) {
        mainShopItemHashMap.clear()
        for(cartItemEntity in mainCartCustomObjectHashMap){
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
            .addSnapshotListener { document, error ->
                error?.printStackTrace()
                if(document!=null){
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
                            order = document.getString(Constants.FIELD_FD_SM_ORDER).toString().toInt(),
                            isClient = document.getString(Constants.FIELD_FD_SM_IS_CLIENT).toString()
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
    }

    private fun initLocationDetectingProcessWithUsersPermissionAsWell(view: View) {
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
//                view.location.text = "আপনার লোকেশন ডিটেক্ট করা যায়নি আবার চেষ্টা করতে ক্লিক করুন"
//                view.location.setOnClickListener {
//                    airLocation.start()
//                }
                airLocation.start()
            }
        },true)
        airLocation.start()
    }

    private fun calculateTotalPrice(view: View) {
        priceTotal = 0
        for(shop in mainShopItemHashMap){
            shop.cart_products.forEach {
                priceTotal += (it.product_item_price*it.product_item_amount)
            }
        }
        setPriceTotalOnView(view)
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
            hideKeyboardFrom(view.context, view)
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
                                        promoCodesArray.filter { promoCode2 ->
                                            if(promoCode2.promoCodeName.equals(view.edt_coupon_code.text.toString(), ignoreCase = true)){
                                                if(promoCode2.active){
                                                    if(promoCode2.onceForOneUser){
                                                        if(!promoCode2.userIds.contains(firebaseAuth.currentUser!!.uid)){
                                                            if(promoCode2.remainingUses!=0){
                                                                if(promoCode2.validityOfCode > System.currentTimeMillis()){
                                                                    if(promoCode2.minimumPrice <= priceTotal){
                                                                        if(promoCode2.shopBased){
                                                                            if(mainShopItemHashMap.size == 1 && mainShopItemHashMap[0].key == promoCode2.shopKey){
                                                                                true
                                                                            }else{
                                                                                errorMessage = "এই প্রমো কোডটি শুধু মাত্র ${promoCode2.shopName} শপের অর্ডারের ক্ষেত্রে প্রযোয্য"
                                                                                false
                                                                            }
                                                                        }else{
                                                                            true
                                                                        }
                                                                    }else{
                                                                        errorMessage = "এই প্রমো কোডের জন্য সর্বনিম্ন ${promoCode2.minimumPrice} টাকার অর্ডার করতে হবে। "
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
                                                            errorMessage = "এই প্রমো কোডটি এক জন ইউজার একবার ই মাত্র ব্যবহার করতে পারবে। "
                                                            false
                                                        }
                                                    }else{
                                                        if(promoCode2.remainingUses!=0){
                                                            if(promoCode2.validityOfCode>System.currentTimeMillis()){
                                                                if(promoCode2.minimumPrice <= priceTotal){
                                                                    if(promoCode2.shopBased){
                                                                        if(mainShopItemHashMap.size == 1 && mainShopItemHashMap[0].key == promoCode2.shopKey){
                                                                            true
                                                                        }else{
                                                                            errorMessage = "এই প্রমো কোডটি শুধু মাত্র ${promoCode2.shopName} শপের অর্ডারের ক্ষেত্রে প্রযোয্য"
                                                                            false
                                                                        }
                                                                    }else{
                                                                        true
                                                                    }
                                                                }else{
                                                                    errorMessage = "এই প্রমো কোডের জন্য সর্বনিম্ন ${promoCode2.minimumPrice} টাকার অর্ডার করতে হবে। "
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
                                    progressDialog.dismiss()
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
    }

    fun sendNotification(userId: String, apititle: String, apibody: String, orderID: String) {
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

    fun hideKeyboardFrom(context: Context, view: View) {
        val imm: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
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