package arpan.delivery.ui.order

import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import arpan.delivery.R
import arpan.delivery.data.adapters.OrderProductItemRecyclerAdapter
import arpan.delivery.data.db.CartProductEntity
import arpan.delivery.data.models.MainShopCartItem
import arpan.delivery.data.models.OrderItemMain
import arpan.delivery.data.models.ShopItem
import arpan.delivery.utils.Constants
import arpan.delivery.utils.createProgressDialog
import arpan.delivery.utils.showToast
import com.baoyachi.stepview.HorizontalStepView
import com.baoyachi.stepview.bean.StepBean
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.kofigyan.stateprogressbar.StateProgressBar
import com.shashank.sony.fancytoastlib.FancyToast
import com.squareup.okhttp.*
import kotlinx.android.synthetic.main.dialog_alert_layout_main.view.*
import kotlinx.android.synthetic.main.fragment_order_history_new.view.*
import kotlinx.android.synthetic.main.product_image_big_view.view.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class OrderHistoryFragmentNew : Fragment() {
    private val mainShopItemArrayList = ArrayList<MainShopCartItem>()
    private val mainShopItemHashMap = ArrayList<MainShopCartItem>()
    private val allOrderItemArrayList = ArrayList<MainShopCartItem>()
    private lateinit var productRecyclerViewAdapter : OrderProductItemRecyclerAdapter
    private lateinit var progressDialog : Dialog
    private lateinit var firebaseFirestore: FirebaseFirestore
    private var orderId = ""
    private var customerId = ""
    private var listenerRegistration : ListenerRegistration? = null
    private var eventListener : EventListener<DocumentSnapshot>? = null
    private var orderItemMain : OrderItemMain? = null
    private var currentCalc = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_order_history_new, container, false)
    }

    override fun onStop() {
        super.onStop()
        if (listenerRegistration != null) {
            listenerRegistration!!.remove()
        }
    }
    override fun onStart() {
        super.onStart()
        if(eventListener!=null){
            listenerRegistration = FirebaseFirestore.getInstance()
                .collection("users")
                .document(customerId)
                .collection("users_order_collection")
                .document(orderId)
                .addSnapshotListener(eventListener!!)
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initVars(view)
        if(FirebaseAuth.getInstance().currentUser == null){
            view.orderHistoryProgressBarContainer.visibility = View.GONE
            view.noDataFoundLinearLayoutContainer.visibility = View.VISIBLE
            view.mainOrderDetailsDataContainerLinearLayout.visibility = View.GONE
        }else{
            if (listenerRegistration == null ) {
                progressDialog.show()
                listenerRegistration = FirebaseFirestore.getInstance().collection("users")
                    .document(customerId)
                    .collection("users_order_collection")
                    .document(orderId).addSnapshotListener(eventListener!!)
            }
        }

    }

    private fun initVars(view: View) {
        firebaseFirestore = FirebaseFirestore.getInstance()
        progressDialog = view.context.createProgressDialog()
        productRecyclerViewAdapter = OrderProductItemRecyclerAdapter(view.context, mainShopItemHashMap)
        eventListener = EventListener<DocumentSnapshot> { snapshot, e ->
            e?.printStackTrace()
            progressDialog.dismiss()
            if (snapshot != null && snapshot.exists()) {
                view.orderHistoryProgressBarContainer.visibility = View.GONE
                view.noDataFoundLinearLayoutContainer.visibility = View.GONE
                view.mainOrderDetailsDataContainerLinearLayout.visibility = View.VISIBLE
                workWithTheDocumentData(view, snapshot)
            }else{
                view.orderHistoryProgressBarContainer.visibility = View.GONE
                view.noDataFoundLinearLayoutContainer.visibility = View.VISIBLE
                view.mainOrderDetailsDataContainerLinearLayout.visibility = View.GONE
            }
        }
        orderId = arguments?.getString("orderID").toString()
        customerId = FirebaseAuth.getInstance().currentUser!!.uid
    }

    private fun workWithTheDocumentData(view: View, snapshot: DocumentSnapshot) {
        // Here we have the snapshot of the "OrderItemMain" and can freely work with it
        // null check and empty check is already done in the previous stage
        orderItemMain = snapshot.toObject(OrderItemMain::class.java) // Parsing to model class
        orderItemMain!!.docID = snapshot.id

        view.orderHistoryProgressBarContainer.visibility = View.GONE
        view.noDataFoundLinearLayoutContainer.visibility = View.GONE
        view.mainOrderDetailsDataContainerLinearLayout.visibility = View.VISIBLE

        initDefaultViewStates(view)
        setTextOnTextViewsOnMainUi(view, orderItemMain!!)
        setLogicForOrderStatusOnThirdRow(view, orderItemMain!!)
        setTextOnRestOrderDetailsTextView(view, orderItemMain!!)
        setLogicForPlacingOrderItemDetailsAndSeparatingProducts(view, orderItemMain!!)
        setDaPaymentMethodCheck(view, orderItemMain!!)

        view.orderHistoryProgressBarContainer.visibility = View.GONE
        view.noDataFoundLinearLayoutContainer.visibility = View.GONE
        view.mainOrderDetailsDataContainerLinearLayout.visibility = View.VISIBLE
    }

    private fun setDaPaymentMethodCheck(view: View, orderItemMain: OrderItemMain) {
        if(!orderItemMain.paymentCompleted && orderItemMain.paymentRequested){
            if(orderItemMain.daDetails.da_bkash.trim().isNotEmpty()){
                view.orderPaymentCompleteOptionContainer.visibility = View.VISIBLE
                view.daPaymentTextView.text = "বিকাশ নম্বরঃ "+orderItemMain.daDetails.da_bkash
                view.paymentAmountText.text = "পেমেন্ট এমাউন্টঃ "+(orderItemMain.totalPrice+orderItemMain.deliveryCharge)+" টাকা"
                view.daPaymentTextView.setOnClickListener {
                    val clipboard: ClipboardManager? =
                        view.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
                    val clip = ClipData.newPlainText("bKash", orderItemMain.daDetails.da_bkash)
                    clipboard!!.setPrimaryClip(clip)
                    view.context.showToast("Number Copied To Clipboard", FancyToast.SUCCESS)
                }
            }else{
                view.orderPaymentCompleteOptionContainer.visibility = View.GONE
            }
        }else{
            view.orderPaymentCompleteOptionContainer.visibility = View.GONE
        }
    }

    private fun initDefaultViewStates(view: View) {
        view.orderImagePicture.visibility = View.GONE
        view.orderDetailsEdittextContainerLayout.visibility = View.GONE
        view.orderTitleEdittextContainerLayout.visibility = View.GONE
        view.orderNoteEdittextContainerLayout.visibility = View.GONE
        view.orderItemsMainRecyclerView.visibility = View.GONE
        view.appliedPromoCodeButton.visibility = View.GONE
        view.orderPaymentCompleteOptionContainer.visibility = View.GONE
        view.linearLayoutCancelReasonContainer.visibility = View.GONE
    }

    private fun setLogicForPlacingOrderItemDetailsAndSeparatingProducts(view: View, orderItemMain: OrderItemMain) {
        if(orderItemMain.pickDropOrder){
            initPickDropDataLogicAndPlaceDataOnUi(view, orderItemMain)
            }else{
            if(orderItemMain.products.size==1 && !orderItemMain.products[0].product_item){
                when {
                    orderItemMain.products[0].parcel_item -> {
                        placeParcelItemData(view, orderItemMain)
                    }
                    orderItemMain.products[0].medicine_item -> {
                        placeMedicineItemData(view, orderItemMain)
                    }
                    orderItemMain.products[0].custom_order_item -> {
                        placeCustomOrderItemData(view, orderItemMain)
                    }
                }
            }else{
                workWithTheArrayList(orderItemMain.products, view)
            }
        }
    }

    private fun placeCustomOrderItemData(view: View, orderItemMain: OrderItemMain) {
        view.orderTitleEdittextContainerLayout.visibility = View.GONE
        view.orderItemsMainRecyclerView.visibility = View.GONE

        if(orderItemMain.products[0].custom_order_text.trim().isNotEmpty()){
            view.orderDetailsEdittextContainerLayout.visibility = View.VISIBLE
            view.orderDetailsEdittextContainerLayout.hint = "ডিটেইলস"
            view.orderDetailsEdittext.setText(orderItemMain.products[0].custom_order_text)
        }else{
            view.orderDetailsEdittextContainerLayout.visibility = View.GONE
        }
        if(orderItemMain.products[0].custom_order_image.isNotEmpty()){
            view.orderImagePicture.visibility = View.VISIBLE
            val firebaseStorage = FirebaseStorage.getInstance()
                .reference.child("ORDER_IMAGES")
                .child(orderItemMain.key)
                .child(orderItemMain.products[0].custom_order_image)
            setOrderImageOnView(view, firebaseStorage)
        }else{
            view.orderImagePicture.visibility = View.GONE
        }
    }
    private fun placeMedicineItemData(view: View, orderItemMain: OrderItemMain) {
        view.orderItemsMainRecyclerView.visibility = View.GONE
        if(orderItemMain.products[0].medicine_order_text.trim().isNotEmpty()){
            view.orderTitleEdittextContainerLayout.visibility = View.VISIBLE
            view.orderTitleEdittextContainerLayout.hint = "ফার্মেসি"
            view.orderTitleEdittext.setText(orderItemMain.products[0].medicine_order_text)
        }else{
            view.orderTitleEdittextContainerLayout.visibility = View.GONE
        }
        if(orderItemMain.products[0].medicine_order_text_2.trim().isNotEmpty()){
            view.orderDetailsEdittextContainerLayout.visibility = View.VISIBLE
            view.orderDetailsEdittextContainerLayout.hint = "ঔষধ"
            view.orderDetailsEdittext.setText(orderItemMain.products[0].medicine_order_text_2)
        }else{
            view.orderDetailsEdittextContainerLayout.visibility = View.GONE
        }
        if(orderItemMain.products[0].medicine_order_image.isNotEmpty()){
            view.orderImagePicture.visibility = View.VISIBLE
            val firebaseStorage = FirebaseStorage.getInstance()
                .reference.child("ORDER_IMAGES")
                .child(orderItemMain.key)
                .child(orderItemMain.products[0].medicine_order_image)
            setOrderImageOnView(view, firebaseStorage)
        }else{
            view.orderImagePicture.visibility = View.GONE
        }
    }
    private fun placeParcelItemData(view: View, orderItemMain: OrderItemMain) {
        view.orderItemsMainRecyclerView.visibility = View.GONE

        if(orderItemMain.products[0].parcel_order_text.trim().isNotEmpty()){
            view.orderTitleEdittextContainerLayout.visibility = View.VISIBLE
            view.orderTitleEdittextContainerLayout.hint = "কুরিয়ার নেম"
            view.orderTitleEdittext.setText(orderItemMain.products[0].parcel_order_text)
        }else{
            view.orderTitleEdittextContainerLayout.visibility = View.GONE
        }
        if(orderItemMain.products[0].parcel_order_text_2.trim().isNotEmpty()){
            view.orderDetailsEdittextContainerLayout.visibility = View.VISIBLE
            view.orderDetailsEdittextContainerLayout.hint = "ডিটেইলস"
            view.orderDetailsEdittext.setText(orderItemMain.products[0].parcel_order_text_2)
        }else{
            view.orderDetailsEdittextContainerLayout.visibility = View.GONE
        }

        if(orderItemMain.products[0].parcel_order_image.isNotEmpty()){
            view.orderImagePicture.visibility = View.VISIBLE
            val firebaseStorage = FirebaseStorage.getInstance()
                .reference.child("ORDER_IMAGES")
                .child(orderItemMain.key)
                .child(orderItemMain.products[0].parcel_order_image)
            setOrderImageOnView(view, firebaseStorage)
        }else{
            view.orderImagePicture.visibility = View.GONE
        }
    }
    private fun initPickDropDataLogicAndPlaceDataOnUi(view: View, orderItemMain: OrderItemMain) {
        view.orderTitleEdittextContainerLayout.visibility = View.GONE
        view.orderDetailsEdittextContainerLayout.visibility = View.VISIBLE
        view.orderItemsMainRecyclerView.visibility = View.GONE
        view.userNameEditText.setText(orderItemMain.pickDropOrderItem.senderName)
        view.userMobileEdittext.setText(orderItemMain.pickDropOrderItem.senderPhone)
        view.userLocationEdittext.setText(orderItemMain.pickDropOrderItem.senderName)
        view.orderDetailsEdittextContainerLayout.hint = "পিক-আপ এন্ড ড্রপ"
        view.orderDetailsEdittext.setText(
            StringBuilder()
                .append("প্রেরকের তথ্যঃ ")
                .append("\n")
                .append("নামঃ ")
                .append(orderItemMain.pickDropOrderItem.senderName)
                .append("\n")
                .append("মোবাইলঃ ")
                .append(orderItemMain.pickDropOrderItem.senderPhone)
                .append("\n")
                .append("ঠিকানাঃ ")
                .append(orderItemMain.pickDropOrderItem.senderAddress)
                .append("\n")
                .append("লোকেশনঃ")
                .append(orderItemMain.pickDropOrderItem.senderLocation)
                .append("\n")
                .append("\n")
                .append("প্রাপকের তথ্যঃ ")
                .append("\n")
                .append("নামঃ ")
                .append(orderItemMain.pickDropOrderItem.recieverName)
                .append("\n")
                .append("মোবাইলঃ ")
                .append(orderItemMain.pickDropOrderItem.recieverPhone)
                .append("\n")
                .append("ঠিকানাঃ ")
                .append(orderItemMain.pickDropOrderItem.recieverAddress)
                .append("\n")
                .append("লোকেশনঃ")
                .append(orderItemMain.pickDropOrderItem.recieverLocation)
                .append("\n")
                .append("\n")
                .append("পার্সেলের তথ্যঃ ")
                .append("\n")
                .append(orderItemMain.pickDropOrderItem.parcelDetails))

        if(orderItemMain.pickDropOrderItem.parcelImage.isNotEmpty()){
            view.orderImagePicture.visibility = View.VISIBLE
            val firebaseStorage = FirebaseStorage.getInstance()
                .reference.child("ORDER_IMAGES")
                .child(orderItemMain.key)
                .child(orderItemMain.pickDropOrderItem.parcelImage)
            setOrderImageOnView(view, firebaseStorage)
        }else{
            view.orderImagePicture.visibility = View.GONE
        }
    }
    private fun setOrderImageOnView(view: View, firebaseStorage: StorageReference) {
        Glide.with(requireActivity())
            .load(firebaseStorage)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerInside()
            .placeholder(R.drawable.loading_image_glide)
            .into(view.orderImagePicture)

        view.orderImagePicture.setOnClickListener {
            val dialog = AlertDialog.Builder(context, R.style.Theme_ArpanDelivery).create()
            val view2 = LayoutInflater.from(context).inflate(R.layout.product_image_big_view,null)
            view2.floatingActionButton.setOnClickListener{
                dialog.dismiss()
            }
            Glide.with(view.context)
                .load(firebaseStorage)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerInside()
                .into(view2.imageView)
            dialog.setView(view2)
            //dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }
    }

    private fun workWithTheArrayList(products: List<CartProductEntity>, view: View) {
        view.orderTitleEdittextContainerLayout.visibility = View.GONE
        view.orderDetailsEdittextContainerLayout.visibility = View.GONE
        view.orderItemsMainRecyclerView.visibility = View.VISIBLE

        mainShopItemHashMap.clear()
        for(cartItemEntity in products){
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
            currentCalc = 0
            fillUpShopDetailsValueInMainShopItemList(view)
        }
    }
    private fun fillUpShopDetailsValueInMainShopItemList(view: View) {
        firebaseFirestore.collection(Constants.FC_SHOPS_MAIN)
            .document(mainShopItemHashMap[currentCalc].shop_doc_id)
            .get().addOnSuccessListener { document ->
                mainShopItemHashMap[currentCalc].shop_details =
                    if(document.data == null){
                        ShopItem(
                            key = "",
                            name = "SHOP DELETED",
                            categories = "",
                            image = "",
                            cover_image = "",
                            da_charge = "",
                            deliver_charge = "",
                            location = "",
                            username = "",
                            password = "",
                            order = 0,
                            status = ""
                        )
                    }else{
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
                            status = document.getString(Constants.FIELD_FD_SM_STATUS).toString()
                        )
                    }
                if(currentCalc+1 >= mainShopItemHashMap.size){
                    // The data is downloaded all of those
                    view.orderItemsMainRecyclerView.layoutManager = LinearLayoutManager(view.context)
                    view.orderItemsMainRecyclerView.adapter = productRecyclerViewAdapter
                }else{
                    currentCalc ++
                    fillUpShopDetailsValueInMainShopItemList(view)
                }
            }
    }

    private fun setTextOnRestOrderDetailsTextView(view: View, orderItemMain: OrderItemMain) {
        if(orderItemMain.promoCodeApplied){
            view.appliedPromoCodeButton.visibility = View.VISIBLE
            view.appliedPromoCodeButton.text = "প্রোমোকোড অ্যাড করা হয়েছেঃ ${orderItemMain.promoCode.promoCodeName}"
        }else{
            view.appliedPromoCodeButton.visibility = View.GONE
        }
        if(orderItemMain.paymentMethod == "COD"){
            view.pricePaymentStatusMain.text = "COD"
        }else{
            view.pricePaymentStatusMain.text = "bKash"
        }
        view.totalPriceTextViewMain.text = "Total: ${orderItemMain.totalPrice}+${orderItemMain.deliveryCharge} = ${orderItemMain.totalPrice+orderItemMain.deliveryCharge}"
    }

    private fun setLogicForOrderStatusOnThirdRow(view: View, orderItemMain: OrderItemMain) {
        view.step_view_order_progress.visibility = View.VISIBLE
        view.orderStatusTopButton.text = orderItemMain.orderStatus
        view.cancelOrderButton.visibility = View.GONE
        when(orderItemMain.orderStatus){
            "PENDING" -> {
                val setpview5 = view.findViewById<View>(R.id.step_view_order_progress) as HorizontalStepView
                val stepsBeanList: MutableList<StepBean> = ArrayList()
                val stepBean4 = StepBean("     PENDING     ", 1)
                val stepBean0 = StepBean("     VERIFIED     ", -1)
                val stepBean1 = StepBean("     PROCESSING     ", -1)
                val stepBean2 = StepBean("     PICKED UP     ", -1)
                val stepBean3 = StepBean("     COMPLETED     ", -1)
                if(orderItemMain.orderPlacingTimeStamp!=0L){
                    stepBean4.name = "PENDING\n(${getDate(orderItemMain.orderPlacingTimeStamp,"hh:mm")})"
                }
                stepsBeanList.add(stepBean4)
                stepsBeanList.add(stepBean0)
                stepsBeanList.add(stepBean1)
                stepsBeanList.add(stepBean2)
                stepsBeanList.add(stepBean3)
                setStepView(view, setpview5, stepsBeanList)
                view.orderStatusTopButton.setBackgroundColor(Color.parseColor("#FA831B"))
                view.cancelOrderButton.visibility = View.VISIBLE
                view.cancelOrderButton.setOnClickListener {
                    cancelOrderItem(view, orderItemMain)
                }
            }
            "VERIFIED" -> {
                val setpview5 = view.findViewById<View>(R.id.step_view_order_progress) as HorizontalStepView
                val stepsBeanList: MutableList<StepBean> = ArrayList()
                val stepBean4 = StepBean("     PENDING     ", 1)
                val stepBean0 = StepBean("     VERIFIED     ", 1)
                val stepBean1 = StepBean("     PROCESSING     ", -1)
                val stepBean2 = StepBean("     PICKED UP     ", -1)
                val stepBean3 = StepBean("     COMPLETED     ", -1)
                if(orderItemMain.verifiedTimeStampMillis!=0L){
                    stepBean0.name = "VERIFIED\n(${getDate(orderItemMain.verifiedTimeStampMillis,"hh:mm")})"
                }
                if(orderItemMain.orderPlacingTimeStamp!=0L){
                    stepBean4.name = "PENDING\n(${getDate(orderItemMain.orderPlacingTimeStamp,"hh:mm")})"
                }
                stepsBeanList.add(stepBean4)
                stepsBeanList.add(stepBean0)
                stepsBeanList.add(stepBean1)
                stepsBeanList.add(stepBean2)
                stepsBeanList.add(stepBean3)

                setStepView(view, setpview5, stepsBeanList)
                view.orderStatusTopButton.setBackgroundColor(Color.parseColor("#ED9D34"))
            }
            "PROCESSING" -> {
                val setpview5 = view.findViewById<View>(R.id.step_view_order_progress) as HorizontalStepView
                val stepsBeanList: MutableList<StepBean> = ArrayList()
                val stepBean4 = StepBean("     PENDING     ", 1)
                val stepBean0 = StepBean("     VERIFIED     ", 1)
                val stepBean1 = StepBean("     PROCESSING     ", 1)
                val stepBean2 = StepBean("     PICKED UP     ", -1)
                val stepBean3 = StepBean("     COMPLETED     ", -1)
                if(orderItemMain.processingTimeStampMillis!=0L){
                    stepBean1.name = "PROCESSING\n(${getDate(orderItemMain.processingTimeStampMillis,"hh:mm")})"
                }
                if(orderItemMain.verifiedTimeStampMillis!=0L){
                    stepBean0.name = "VERIFIED\n(${getDate(orderItemMain.verifiedTimeStampMillis,"hh:mm")})"
                }
                if(orderItemMain.orderPlacingTimeStamp!=0L){
                    stepBean4.name = "PENDING\n(${getDate(orderItemMain.orderPlacingTimeStamp,"hh:mm")})"
                }
                stepsBeanList.add(stepBean4)
                stepsBeanList.add(stepBean0)
                stepsBeanList.add(stepBean1)
                stepsBeanList.add(stepBean2)
                stepsBeanList.add(stepBean3)

                setStepView(view, setpview5, stepsBeanList)
                view.orderStatusTopButton.setBackgroundColor(Color.parseColor("#ED9D34"))
            }
            "PICKED UP" -> {
                view.step_view_order_progress.visibility = View.VISIBLE
                val setpview5 = view.findViewById<View>(R.id.step_view_order_progress) as HorizontalStepView
                val stepsBeanList: MutableList<StepBean> = ArrayList()
                val stepBean4 = StepBean("     PENDING     ", 1)
                val stepBean0 = StepBean("     VERIFIED     ", 1)
                val stepBean1 = StepBean("     PROCESSING     ", 1)
                val stepBean2 = StepBean("     PICKED UP     ", 1)
                val stepBean3 = StepBean("     COMPLETED     ", -1)
                if(orderItemMain.processingTimeStampMillis!=0L){
                    stepBean1.name = "PROCESSING\n(${getDate(orderItemMain.processingTimeStampMillis,"hh:mm")})"
                }
                if(orderItemMain.verifiedTimeStampMillis!=0L){
                    stepBean0.name = "VERIFIED\n(${getDate(orderItemMain.verifiedTimeStampMillis,"hh:mm")})"
                }
                if(orderItemMain.orderPlacingTimeStamp!=0L){
                    stepBean4.name = "PENDING\n(${getDate(orderItemMain.orderPlacingTimeStamp,"hh:mm")})"
                }
                if(orderItemMain.pickedUpTimeStampMillis!=0L){
                    stepBean2.name = "PICKED UP\n(${getDate(orderItemMain.pickedUpTimeStampMillis,"hh:mm")})"
                }
                stepsBeanList.add(stepBean4)
                stepsBeanList.add(stepBean0)
                stepsBeanList.add(stepBean1)
                stepsBeanList.add(stepBean2)
                stepsBeanList.add(stepBean3)

                setStepView(view, setpview5, stepsBeanList)
                view.orderStatusTopButton.setBackgroundColor(Color.parseColor("#ED9D34"))
            }
            "COMPLETED" -> {
                if(orderItemMain.orderCompletedStatus=="CANCELLED"){
                    view.step_view_order_progress.visibility = View.GONE
                    view.orderStatusTopButton.text = "CANCELLED"
                    if(orderItemMain.cancelledOrderReasonFromAdmin.trim().isNotEmpty()){
                        view.linearLayoutCancelReasonContainer.visibility = View.VISIBLE
                        view.orderCancellationReasonDetails.text = orderItemMain.cancelledOrderReasonFromAdmin
                    }else{
                        view.linearLayoutCancelReasonContainer.visibility = View.GONE
                    }
                    view.orderStatusTopButton.setBackgroundColor(Color.parseColor("#EA594D"))
                }else{
                    val setpview5 = view.findViewById<View>(R.id.step_view_order_progress) as HorizontalStepView
                    val stepsBeanList: MutableList<StepBean> = ArrayList()
                    val stepBean4 = StepBean("     PENDING     ", 1)
                    val stepBean0 = StepBean("     VERIFIED     ", 1)
                    val stepBean1 = StepBean("     PROCESSING     ", 1)
                    val stepBean2 = StepBean("     PICKED UP     ", 1)
                    val stepBean3 = StepBean("     COMPLETED     ", 1)
                    if(orderItemMain.processingTimeStampMillis!=0L){
                        stepBean1.name = "PROCESSING\n(${getDate(orderItemMain.processingTimeStampMillis,"hh:mm")})"
                    }
                    if(orderItemMain.verifiedTimeStampMillis!=0L){
                        stepBean0.name = "VERIFIED\n(${getDate(orderItemMain.verifiedTimeStampMillis,"hh:mm")})"
                    }
                    if(orderItemMain.orderPlacingTimeStamp!=0L){
                        stepBean4.name = "PENDING\n(${getDate(orderItemMain.orderPlacingTimeStamp,"hh:mm")})"
                    }
                    if(orderItemMain.pickedUpTimeStampMillis!=0L){
                        stepBean2.name = "PICKED UP\n(${getDate(orderItemMain.pickedUpTimeStampMillis,"hh:mm")})"
                    }
                    if(orderItemMain.completedTimeStampMillis!=0L){
                        stepBean3.name = "COMPLETED\n(${getDate(orderItemMain.completedTimeStampMillis,"hh:mm")})"
                    }
                    stepsBeanList.add(stepBean4)
                    stepsBeanList.add(stepBean0)
                    stepsBeanList.add(stepBean1)
                    stepsBeanList.add(stepBean2)
                    stepsBeanList.add(stepBean3)

                    setStepView(view, setpview5, stepsBeanList)
                    view.orderStatusTopButton.setBackgroundColor(Color.parseColor("#43A047"))
                }
            }
        }
    }

    private fun setStepView(view:View ,setpview5: HorizontalStepView, stepsBeanList: MutableList<StepBean>) {
        setpview5.setStepViewTexts(stepsBeanList) //总步骤
            .setTextSize(10) //set textSize
            .setStepsViewIndicatorCompletedLineColor(ContextCompat.getColor(view.context,
                R.color.blue_normal
            )) //设置StepsViewIndicator完成线的颜色
            .setStepsViewIndicatorUnCompletedLineColor(ContextCompat.getColor(view.context, R.color.blue_normal)) //设置StepsViewIndicator未完成线的颜色
            .setStepViewComplectedTextColor(ContextCompat.getColor(view.context!!, R.color.grey_normal)) //设置StepsView text完成线的颜色
            .setStepViewUnComplectedTextColor(ContextCompat.getColor(view.context, R.color.grey_normal)) //设置StepsView text未完成线的颜色
            .setStepsViewIndicatorCompleteIcon(ContextCompat.getDrawable(view.context,
                R.drawable.ic_baseline_checked
            )) //设置StepsViewIndicator CompleteIcon
            .setStepsViewIndicatorDefaultIcon(ContextCompat.getDrawable(view.context,
                R.drawable.unchecked_bg_stroked
            )) //设置StepsViewIndicator DefaultIcon
            .setStepsViewIndicatorAttentionIcon(ContextCompat.getDrawable(view.context, R.drawable.attention)) //设置StepsViewIndicator AttentionIcon
    }

    fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)
        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar: Calendar = Calendar.getInstance()
        calendar.setTimeInMillis(milliSeconds)
        return formatter.format(calendar.getTime())
    }

    private fun cancelOrderItem(view: View, orderItemMain: OrderItemMain) {
        val view = LayoutInflater.from(view.context)
            .inflate(R.layout.dialog_alert_layout_main, null)
        val dialog = AlertDialog.Builder(view.context)
            .setView(view).create()
        view.btnNoDialogAlertMain.text = getString(R.string.no)
        view.btnYesDialogAlertMain.text = getString(R.string.ok_text)
        view.titleTextView.text = "আপনি কি নিশ্চিত?"
        view.messageTextView.text = "অর্ডারটি ক্যান্সেল করতে চান?"
        view.btnNoDialogAlertMain.setOnClickListener {
            dialog.dismiss()
        }
        view.btnYesDialogAlertMain.setOnClickListener {
            dialog.dismiss()
            progressDialog.show()
            val hashMap = HashMap<String,Any>()
            hashMap["orderStatus"] = "COMPLETED"
            hashMap["orderCompletedStatus"] = "CANCELLED"
            hashMap["cancelledOrderReasonFromAdmin"] = "আপনার অর্ডারটি আপনি নিজেই ক্যান্সেল করে দিয়েছেন। ধন্যবাদ।"
            FirebaseFirestore.getInstance().collection("users")
                .document(customerId)
                .collection("users_order_collection")
                .document(orderId)
                .update(hashMap)
                .addOnCompleteListener {task2 ->
                    progressDialog.dismiss()
                }
        }
        dialog.show()
    }



    private fun setTextOnTextViewsOnMainUi(view: View, orderItemMain: OrderItemMain) {
        view.orderIdTextView.text = "Order# "+orderItemMain.orderId

        view.userNameEditText.setText(orderItemMain.userName)
        view.userMobileEdittext.setText(orderItemMain.userNumber)

        if(orderItemMain.locationItem.locationName.trim()=="মাগুরা সদর"){
            if(orderItemMain.userAddress.trim().isNotEmpty()){
                view.userAddressEdittextContainerLayout.visibility = View.VISIBLE
                view.userAddressEdittext.setText(orderItemMain.userAddress)
            }else{
                view.userAddressEdittextContainerLayout.visibility = View.GONE
            }
        }else{
            view.userAddressEdittextContainerLayout.visibility = View.GONE
        }

        view.userLocationEdittext.setText(orderItemMain.locationItem.locationName)

        view.userNameEditText.setText(orderItemMain.userName)

        if(orderItemMain.userNote.trim().isNotEmpty()){
            view.orderNoteEdittextContainerLayout.visibility = View.VISIBLE
            view.orderNoteEdittext.setText(orderItemMain.userNote)
        }else{
            view.orderNoteEdittextContainerLayout.visibility = View.GONE
        }
    }

    fun sendNotificationToDa(userId: String, daId: String, apititle: String, apibody: String, orderID: String) {
        val mediaType: MediaType =
            MediaType.parse("application/json; charset=utf-8")
        val data: MutableMap<String, String> =
            java.util.HashMap()
        data["userId"] = userId
        data["daId"] = daId
        data["apititle"] = apititle
        data["apibody"] = apibody
        data["orderID"] = orderID
        data["click_action"] = ".ui.home.HomeActivity"
        val json = Gson().toJson(data)
        val body: RequestBody =
            RequestBody.create(
                mediaType,
                json
            )
        val request: Request = Request.Builder()
            .url("https://arpan-fcm.herokuapp.com/send-notification-to-da-about-a-new-order-that-he-recieved")
            .post(body)
            .build()
        OkHttpClient().newCall(request).enqueue(object : Callback{
            override fun onFailure(request: Request?, e: IOException?) {
                e!!.printStackTrace()
            }

            override fun onResponse(response: Response?) {
                Log.e("notifiication response" , response!!.message())
            }

        })
    }
}