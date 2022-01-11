package arpan.delivery.ui.order

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import arpan.delivery.R
import arpan.delivery.data.adapters.OrderItemRecyclerAdapter
import arpan.delivery.data.adapters.OrderProductItemRecyclerAdapter
import arpan.delivery.data.db.CartProductEntity
import arpan.delivery.data.models.MainShopCartItem
import arpan.delivery.data.models.OrderItemMain
import arpan.delivery.data.models.PromoCode
import arpan.delivery.data.models.ShopItem
import arpan.delivery.ui.home.HomeActivity
import arpan.delivery.ui.home.HomeViewModel
import arpan.delivery.utils.Constants
import arpan.delivery.utils.createProgressDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.fragment_order_history.view.*
import kotlinx.android.synthetic.main.fragment_order_history.view.imageView
import kotlinx.android.synthetic.main.product_image_big_view.view.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class OrderHistoryFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val mainCartCustomObjectHashMap = HashMap<String, ArrayList<CartProductEntity>>()
    private val mainShopItemHashMap = ArrayList<MainShopCartItem>()
    private lateinit var productRecyclerViewAdapter : OrderProductItemRecyclerAdapter
    private lateinit var progressDialog : Dialog
    private lateinit var firebaseFirestore: FirebaseFirestore
    private var currentCalc = 0
    private var priceTotal = 0
    private var deliveryCharge = 0
    private var promoCodeActive = false
    private var promoCode = PromoCode()

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
        return inflater.inflate(R.layout.fragment_order_history, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment OrderHistoryFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            OrderHistoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (view.context as HomeActivity).titleActionBarTextView.text = getString(R.string.order_details)
        (view.context as HomeActivity).deleteItemsFromCart.visibility = View.GONE
        (view.context as HomeActivity).img_cart_icon.visibility = View.INVISIBLE
        initVars(view)
        val orderId = arguments?.getString("orderID").toString()
        if(FirebaseAuth.getInstance().currentUser == null){
            view.shopsProgress.visibility = View.GONE
            view.noProductsText.visibility = View.VISIBLE
            view.mainLayout.visibility = View.GONE
        }else{
            progressDialog.show()
            FirebaseFirestore.getInstance().collection("users")
                    .document(FirebaseAuth.getInstance().currentUser!!.uid)
                    .collection("users_order_collection")
                    .document(orderId)
                    .get().addOnCompleteListener {
                    progressDialog.dismiss()
                    if(it.isSuccessful){
                            if(it.result!!.exists()){
                                val orderItemMain = it.result!!.toObject(OrderItemMain::class.java) as OrderItemMain
                                if(orderItemMain.pickDropOrder){
                                    view.nestedScrollView.visibility = View.GONE
                                    view.pickDropScrollView.visibility = View.VISIBLE
                                    view.text_name_container.visibility = View.GONE
                                    view.text_number_container.visibility = View.GONE
                                    view.edt_name.setText(orderItemMain.pickDropOrderItem.senderName)
                                    view.edt_mobile.setText(orderItemMain.pickDropOrderItem.senderPhone)
                                    view.edt_address.setText(orderItemMain.pickDropOrderItem.senderAddress)
                                    view.edt_aboutParcel.setText(orderItemMain.pickDropOrderItem.parcelDetails)
                                    view.edt_name_reciver.setText(orderItemMain.pickDropOrderItem.recieverName)
                                    view.edt_mobile_reciver.setText(orderItemMain.pickDropOrderItem.recieverPhone)
                                    view.edt_address_reciver.setText(orderItemMain.pickDropOrderItem.recieverAddress)
                                    view.txt_number.visibility = View.GONE
                                }else{
                                    view.text_number_container.visibility = View.VISIBLE
                                    view.text_name_container.visibility = View.VISIBLE
                                    view.txt_number.visibility = View.VISIBLE
                                    if(orderItemMain.products.size==1 && !orderItemMain.products[0].product_item){
                                        view.customOrderLinearLayout.visibility = View.VISIBLE
                                        view.nestedScrollView.visibility = View.GONE
                                        view.pickDropScrollView.visibility = View.GONE
                                        if(orderItemMain.products[0].parcel_item){
                                            view.titleCustomOrderLinear.text = "পার্সেল অর্ডার"
                                            view.customOrderTitleTextView.text = "কুরিয়ার নেমঃ "+ orderItemMain.products[0].parcel_order_text
                                            view.customOrderDetailsTextView.text = "ডিটেইলসঃ "+ orderItemMain.products[0].parcel_order_text_2
                                            if(orderItemMain.products[0].parcel_order_image.isNotEmpty()){
                                                val firebaseStorage = FirebaseStorage.getInstance()
                                                    .reference.child("ORDER_IMAGES")
                                                    .child(orderItemMain.key)
                                                    .child(orderItemMain.products[0].parcel_order_image)

                                                Glide.with(requireContext())
                                                    .load(firebaseStorage)
                                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                    .centerCrop()
                                                    .override(300,300)
                                                    .placeholder(R.drawable.loading_image_glide).into(view.shopImageItem)

                                                view.shopImageItem.setOnClickListener {
                                                    val dialog = AlertDialog.Builder(context, R.style.Theme_ArpanDelivery).create()
                                                    val view2 = LayoutInflater.from(context).inflate(R.layout.product_image_big_view,null)
                                                    view2.floatingActionButton.setOnClickListener{
                                                        dialog.dismiss()
                                                    }
                                                    Glide.with(requireContext())
                                                        .load(firebaseStorage)
                                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                        .centerInside()
                                                        .into(view2.imageView)
                                                    dialog.setView(view2)
                                                    //dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                                    dialog.show()
                                                }
                                            }else{
                                                view.shopImageItem.visibility = View.GONE
                                            }
                                        }else if(orderItemMain.products[0].medicine_item){
                                            view.titleCustomOrderLinear.text = "মেডিসিন অর্ডার"
                                            view.customOrderTitleTextView.text = "ফার্মেসিঃ "+ orderItemMain.products[0].medicine_order_text
                                            view.customOrderDetailsTextView.text = "ঔষোধঃ "+ orderItemMain.products[0].medicine_order_text_2
                                            if(orderItemMain.products[0].medicine_order_image.isNotEmpty()){
                                                val firebaseStorage = FirebaseStorage.getInstance()
                                                    .reference.child("ORDER_IMAGES")
                                                    .child(orderItemMain.key)
                                                    .child(orderItemMain.products[0].medicine_order_image)

                                                Glide.with(requireContext())
                                                    .load(firebaseStorage)
                                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                    .centerCrop()
                                                    .override(300,300)
                                                    .placeholder(R.drawable.loading_image_glide).into(view.shopImageItem)

                                                view.shopImageItem.setOnClickListener {
                                                    val dialog = AlertDialog.Builder(context, R.style.Theme_ArpanDelivery).create()
                                                    val view2 = LayoutInflater.from(context).inflate(R.layout.product_image_big_view,null)
                                                    view2.floatingActionButton.setOnClickListener{
                                                        dialog.dismiss()
                                                    }
                                                    Glide.with(requireContext())
                                                        .load(firebaseStorage)
                                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                        .centerInside()
                                                        .into(view2.imageView)
                                                    dialog.setView(view2)
                                                    //dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                                    dialog.show()
                                                }
                                            }else{
                                                view.shopImageItem.visibility = View.GONE
                                            }
                                        }else if(orderItemMain.products[0].custom_order_item){
                                            view.titleCustomOrderLinear.text = "কাস্টম অর্ডার"
                                            view.customOrderTitleTextView.text = "জেনারেল অর্ডার"
                                            view.customOrderDetailsTextView.text = "ডিটেইলসঃ "+ orderItemMain.products[0].custom_order_text
                                            if(orderItemMain.products[0].custom_order_image.isNotEmpty()){
                                                val firebaseStorage = FirebaseStorage.getInstance()
                                                    .reference.child("ORDER_IMAGES")
                                                    .child(orderItemMain.key)
                                                    .child(orderItemMain.products[0].custom_order_image)

                                                Glide.with(requireContext())
                                                    .load(firebaseStorage)
                                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                    .centerCrop()
                                                    .override(300,300)
                                                    .placeholder(R.drawable.loading_image_glide).into(view.shopImageItem)

                                                view.shopImageItem.setOnClickListener {
                                                    val dialog = AlertDialog.Builder(context, R.style.Theme_ArpanDelivery).create()
                                                    val view2 = LayoutInflater.from(context).inflate(R.layout.product_image_big_view,null)
                                                    view2.floatingActionButton.setOnClickListener{
                                                        dialog.dismiss()
                                                    }
                                                    Glide.with(requireContext())
                                                        .load(firebaseStorage)
                                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                        .centerInside()
                                                        .into(view2.imageView)
                                                    dialog.setView(view2)
                                                    //dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                                    dialog.show()
                                                }
                                            }else{
                                                view.shopImageItem.visibility = View.GONE
                                            }
                                        }
                                    }else{
                                        view.nestedScrollView.visibility = View.VISIBLE
                                        view.pickDropScrollView.visibility = View.GONE
                                        view.customOrderLinearLayout.visibility = View.GONE
                                        workWithTheArrayList(orderItemMain.products, view)
                                    }
                                }
                                if(orderItemMain.userAddress.isEmpty()){
                                    view.text_address_container.visibility = View.GONE
                                }else{
                                    view.text_address_container.visibility = View.VISIBLE
                                    view.txt_address.setText(orderItemMain.userAddress)
                                }
                                priceTotal = orderItemMain.totalPrice
                                deliveryCharge = orderItemMain.deliveryCharge
                                promoCodeActive = orderItemMain.promoCodeApplied
                                promoCode = orderItemMain.promoCode
                                setPriceTotalOnView(view)
                                view.orderIDText.text = orderItemMain.orderId
                                if(orderItemMain.orderCompletedStatus == "CANCELLED") {
                                    view.orderStatusText2.text = "CANCELLED"
                                    view.button.text = "CANCELLED"
                                }else{
                                    view.orderStatusText2.text = orderItemMain.orderStatus
                                    view.button.text = orderItemMain.orderStatus
                                }
                                view.txt_name.setText(orderItemMain.userName)
                                view.txt_number.setText(orderItemMain.userNumber)
                                if(orderItemMain.userAddress.isEmpty()){
                                    view.text_address_container.visibility = View.GONE
                                }else{
                                    view.text_address_container.visibility = View.VISIBLE
                                    view.txt_address.setText(orderItemMain.userAddress)
                                }
                                if(orderItemMain.userNote.isEmpty()){
                                    view.text_note_container.visibility = View.GONE
                                }else{
                                    view.text_note_container.visibility = View.VISIBLE
                                    view.txt_note.setText(orderItemMain.userNote)
                                }
                                if(orderItemMain.pickDropOrderItem.parcelImage.isEmpty()){
                                    view.cardViewPickDropImaage.visibility = View.GONE
                                }else{
                                    view.cardViewPickDropImaage.visibility = View.VISIBLE
                                    val storageReference = FirebaseStorage.getInstance().reference.child("ORDER_IMAGES")
                                            .child(orderItemMain.key).child(orderItemMain.pickDropOrderItem.parcelImage)

                                    Glide.with(view.context)
                                            .load(storageReference)
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .centerCrop()
                                            .override(300,300)
                                            .placeholder(R.drawable.loading_image_glide).into(view.imageView)
                                }
                                view.paymentText.text = getString(R.string.payment_method) +" "+ orderItemMain.paymentMethod
                                view.shopsProgress.visibility = View.GONE
                                view.noProductsText.visibility = View.GONE
                                view.mainLayout.visibility = View.VISIBLE
                            }else{
                                view.shopsProgress.visibility = View.GONE
                                view.noProductsText.visibility = View.VISIBLE
                                view.mainLayout.visibility = View.GONE
                            }
                        }else{
                            view.shopsProgress.visibility = View.GONE
                            view.noProductsText.visibility = View.VISIBLE
                            view.mainLayout.visibility = View.GONE
                        }
                    }
        }

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
            initiateRestLogicForArrayList(view)
        }else{
            view.productsTextView.visibility = View.GONE
            view.productsRecyclerView.visibility = View.GONE
        }
        if(mainCartCustomObjectHashMap["parcel_item"]!!.isNotEmpty()){
            view.parcelOrderTextView.visibility = View.VISIBLE
            view.parcelOrderTextView2.visibility = View.VISIBLE
            view.parcelRecyclerView.visibility = View.VISIBLE
            initiateRestLogicForParcel(view)
        }else{
            view.parcelOrderTextView.visibility = View.GONE
            view.parcelOrderTextView2.visibility = View.GONE
            view.parcelRecyclerView.visibility = View.GONE
        }
        if(mainCartCustomObjectHashMap["custom_order_item"]!!.isNotEmpty()){
            view.customOrderTextView.visibility = View.VISIBLE
            view.customOrderTextView2.visibility = View.VISIBLE
            view.customOrderRecyclerView.visibility = View.VISIBLE
            initiateRestLogicForCustomOrder(view)
        }else{
            view.customOrderTextView.visibility = View.GONE
            view.customOrderTextView2.visibility = View.GONE
            view.customOrderRecyclerView.visibility = View.GONE
        }
        if(mainCartCustomObjectHashMap["medicine_item"]!!.isNotEmpty()){
            view.medicineOrderTextView.visibility = View.VISIBLE
            view.medicineOrderTextView2.visibility = View.VISIBLE
            view.medicineRecyclerView.visibility = View.VISIBLE
            initiateRestLogicForMedicine(view)
        }else{
            view.medicineOrderTextView.visibility = View.GONE
            view.medicineOrderTextView2.visibility = View.GONE
            view.medicineRecyclerView.visibility = View.GONE
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
                                order = 0
                            )
                        } else {
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
                        }
                    if(currentCalc+1 >= mainShopItemHashMap.size){
                        // The data is downloaded all of those
                        view.productsRecyclerView.layoutManager = LinearLayoutManager(view.context)
                        view.productsRecyclerView.adapter = productRecyclerViewAdapter
                        progressDialog.dismiss()
                    }else{
                        currentCalc ++
                        fillUpShopDetailsValueInMainShopItemList(view)
                    }
                }
    }

    private fun setPriceTotalOnView(view: View) {
        view.txtAllPrice.text = getString(R.string.total_total_text)+" ${priceTotal}+${deliveryCharge} " +
                "= ${priceTotal+deliveryCharge} "+getString(R.string.taka_text)
        if(promoCodeActive){
            view.promoCodeAppliedLinear.visibility = View.VISIBLE
            view.promoCodeAppliedText.text  = getString(R.string.you_got_part_1)+" "+promoCode.discountPrice+" "+getString(R.string.you_got_part_2)
        }else{
            view.promoCodeAppliedLinear.visibility = View.GONE
        }
    }

    private fun initVars(view: View) {
        firebaseFirestore = FirebaseFirestore.getInstance()
        progressDialog = view.context.createProgressDialog()
        productRecyclerViewAdapter = OrderProductItemRecyclerAdapter(view.context, mainShopItemHashMap)
        mainCartCustomObjectHashMap["product_item"] = ArrayList()
        mainCartCustomObjectHashMap["parcel_item"] = ArrayList()
        mainCartCustomObjectHashMap["custom_order_item"] = ArrayList()
        mainCartCustomObjectHashMap["medicine_item"] = ArrayList()
    }

    override fun onResume() {
        super.onResume()
    }
}