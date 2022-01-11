package arpan.delivery.ui.cart

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import arpan.delivery.R
import arpan.delivery.data.adapters.CartItemRecyclerAdapter
import arpan.delivery.data.adapters.CartProductItemRecyclerAdapter
import arpan.delivery.data.db.CartProductEntity
import arpan.delivery.data.models.MainShopCartItem
import arpan.delivery.data.models.ShopItem
import arpan.delivery.ui.auth.PhoneAuthActivity
import arpan.delivery.ui.home.HomeActivity
import arpan.delivery.ui.launcher.MainActivity
import arpan.delivery.utils.Constants
import arpan.delivery.utils.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.shashank.sony.fancytoastlib.FancyToast
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.dialog_alert_layout_main.view.*
import kotlinx.android.synthetic.main.fragment_cart.view.*
import kotlinx.android.synthetic.main.fragment_cart.view.productsRecyclerView
import kotlinx.android.synthetic.main.fragment_order_new.view.*
import mumayank.com.airlocationlibrary.AirLocation
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CartFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CartFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var cartViewModel : CartViewModel
    private val mainCartCustomObjectHashMap = HashMap<String,ArrayList<CartProductEntity>>()
    private val mainShopItemHashMap = ArrayList<MainShopCartItem>()
    private var currentCalc = 0
    private val firebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var productRecyclerViewAdapter : CartProductItemRecyclerAdapter

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
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CartFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CartFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (view.context as HomeActivity).titleActionBarTextView.text = getString(R.string.cart_page_title)
        (view.context as HomeActivity).deleteItemsFromCart.visibility = View.VISIBLE
        (view.context as HomeActivity).img_cart_icon.visibility = View.GONE
        initVars(view)
        initLogic(view)
    }

    private fun initVars(view: View) {
        cartViewModel = (view.context as HomeActivity).cartViewModel
        productRecyclerViewAdapter =
                CartProductItemRecyclerAdapter(view.context, mainShopItemHashMap)
        mainCartCustomObjectHashMap["product_item"] = ArrayList()
        mainCartCustomObjectHashMap["parcel_item"] = ArrayList()
        mainCartCustomObjectHashMap["custom_order_item"] = ArrayList()
        mainCartCustomObjectHashMap["medicine_item"] = ArrayList()
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
        if(mainCartCustomObjectHashMap["product_item"]!!.isEmpty()&&
                mainCartCustomObjectHashMap["parcel_item"]!!.isEmpty()&&
                mainCartCustomObjectHashMap["custom_order_item"]!!.isEmpty()&&
                mainCartCustomObjectHashMap["medicine_item"]!!.isEmpty()){
            view.textLinearView.visibility = View.VISIBLE
            view.recyclersLinearView.visibility = View.GONE
            view.floating_action_button.visibility = View.GONE
        }else{
            view.textLinearView.visibility = View.GONE
            view.recyclersLinearView.visibility = View.VISIBLE
            var snapshot = (view.context as HomeActivity).dataSnapshotOrderTakingTime
            val startTime = snapshot.child("start_time").value.toString()
            val endTime = snapshot.child("end_time").value.toString()
            val over_time_orders = snapshot.child("over_time_orders").value.toString()
            if(over_time_orders == "yes"){
                view.floating_action_button.visibility = View.VISIBLE
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
                    view.floating_action_button.visibility = View.VISIBLE
                }else{
                    view.context.showToast(getString(R.string.shgop_order_time_ehon_bodro_ache), FancyToast.ERROR)
                    view.floating_action_button.visibility = View.GONE
                }
            }
            if(mainCartCustomObjectHashMap["product_item"]!!.isNotEmpty()){
                view.productsRecyclerView.visibility = View.VISIBLE
                initiateRestLogicForArrayList(view)
            }else{
                view.productsRecyclerView.visibility = View.GONE
            }
            if(mainCartCustomObjectHashMap["parcel_item"]!!.isNotEmpty()){
                view.parcelOrderTextView.visibility = View.VISIBLE
                view.parcelRecyclerView.visibility = View.VISIBLE
                initiateRestLogicForParcel(view)
            }else{
                view.parcelOrderTextView.visibility = View.GONE
                view.parcelRecyclerView.visibility = View.GONE
            }
            if(mainCartCustomObjectHashMap["custom_order_item"]!!.isNotEmpty()){
                view.customOrderTextView.visibility = View.VISIBLE
                view.customOrderRecyclerView.visibility = View.VISIBLE
                initiateRestLogicForCustomOrder(view)
            }else{
                view.customOrderTextView.visibility = View.GONE
                view.customOrderRecyclerView.visibility = View.GONE
            }
            if(mainCartCustomObjectHashMap["medicine_item"]!!.isNotEmpty()){
                view.medicineOrderTextView.visibility = View.VISIBLE
                view.medicineRecyclerView.visibility = View.VISIBLE
                initiateRestLogicForMedicine(view)
            }else{
                view.medicineOrderTextView.visibility = View.GONE
                view.medicineRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun initiateRestLogicForMedicine(view: View) {
//        val cartItemRecyclerAdapter = mainCartCustomObjectHashMap["medicine_item"]?.let { CartItemRecyclerAdapter(view.context, it) }
//        view.medicineRecyclerView.layoutManager = LinearLayoutManager(view.context)
//        view.medicineRecyclerView.adapter = cartItemRecyclerAdapter
    }

    private fun initiateRestLogicForCustomOrder(view: View) {
//        val cartItemRecyclerAdapter = mainCartCustomObjectHashMap["custom_order_item"]?.let { CartItemRecyclerAdapter(view.context, it) }
//        view.customOrderRecyclerView.layoutManager = LinearLayoutManager(view.context)
//        view.customOrderRecyclerView.adapter = cartItemRecyclerAdapter
    }

    private fun initiateRestLogicForParcel(view: View) {
//        val cartItemRecyclerAdapter = mainCartCustomObjectHashMap["parcel_item"]?.let { CartItemRecyclerAdapter(view.context,it) }
//        view.parcelRecyclerView.layoutManager = LinearLayoutManager(view.context)
//        view.parcelRecyclerView.adapter = cartItemRecyclerAdapter
    }

    private fun initiateRestLogicForArrayList(view: View) {
        mainShopItemHashMap.clear()
        for(cartItemEntity in mainCartCustomObjectHashMap["product_item"]!!){
            val filteredArray = mainShopItemHashMap.filter {
                it -> it.shop_doc_id == cartItemEntity.product_item_shop_key }
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
            showProgressDialog(view.context)
            currentCalc = 0
            fillUpShopDetailsValueInMainShopItemList(view)
        }
    }

    private fun fillUpShopDetailsValueInMainShopItemList(view: View) {
        firebaseFirestore.collection(Constants.FC_SHOPS_MAIN)
                .document(mainShopItemHashMap[currentCalc].shop_doc_id)
                .get(Source.CACHE).addOnSuccessListener { document ->
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
                        hideProgressDialog(view.context)
                    }else{
                        currentCalc ++
                        fillUpShopDetailsValueInMainShopItemList(view)
                    }
                }
    }

    private fun initLogic(view: View) {
        var locationAsked = false
        if(FirebaseAuth.getInstance().currentUser!=null){
            view.floating_action_button.text = getString(R.string.order_now)
            view.floating_action_button.setOnClickListener {
                if (locationAsked) {
                    (view.context as HomeActivity).navController.navigate(R.id.action_cartFragment_to_orderFragment)
                } else {
                    val manager = view.context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    locationAsked = true
                    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        AirLocation(view.context as HomeActivity, object : AirLocation.Callback {
                            override fun onSuccess(locations: ArrayList<Location>) {}
                            override fun onFailure(locationFailedEnum: AirLocation.LocationFailedEnum) {}
                        },true).start()
                    }else{
                        (view.context as HomeActivity).navController.navigate(R.id.action_cartFragment_to_orderFragment)
                    }
                }
            }
        }else{
            view.floating_action_button.text = getString(R.string.login_to_order)
            view.floating_action_button.setOnClickListener {
                val i = Intent(view.context, PhoneAuthActivity::class.java)
                view.context.startActivity(i)
            }
        }
        workWithTheArrayList(cartViewModel.cartItems.value!!, view)
    }

    private fun showProgressDialog(context : Context){
        (context as HomeActivity).showProgressDialog()
    }

    private fun hideProgressDialog(context : Context){
        (context as HomeActivity).hideProgressDialog()
    }
}