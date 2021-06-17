package arpan.delivery.ui.order

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import arpan.delivery.R
import arpan.delivery.data.adapters.OrderOldMainItemRecyclerAdapter
import arpan.delivery.data.models.OrderItemMain
import arpan.delivery.data.models.OrderOldItems
import arpan.delivery.ui.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.fragment_old_order_list_fragment.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class OldOrderListFragment : Fragment() {

    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var firebaseStorage: FirebaseStorage
    private var ordersMainArrayList =  ArrayList<OrderItemMain>()
    private var ordersMainHashMap = HashMap<String, ArrayList<OrderItemMain>>()
    private var ordersMainOldItemsArrayList = ArrayList<OrderOldItems>()
    private lateinit var mainView : View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_old_order_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (view.context as HomeActivity).titleActionBarTextView.text = getString(R.string.my_orders)
        (view.context as HomeActivity).deleteItemsFromCart.visibility = View.GONE
        (view.context as HomeActivity).img_cart_icon.visibility = View.INVISIBLE
        initVars(view)
        initLogics(view)
    }

    private fun initLogics(view: View) {
        loadDataFirstTime(view)
    }

    private fun initVars(view: View) {
        firebaseFirestore = FirebaseFirestore.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        mainView = view
    }

    private fun loadDataFirstTime(view: View) {
        if(FirebaseAuth.getInstance().currentUser==null){
            view.noProductsText.visibility = View.VISIBLE
            view.noProductsTextView.text = getString(R.string.you_are_not_logged_i)
            view.progressBar.visibility = View.GONE
            view.recyclerView.visibility = View.GONE
            view.radioGroup.visibility = View.GONE
        }else{
            firebaseFirestore.collection("users")
                    .document(FirebaseAuth.getInstance().currentUser!!.uid)
                    .collection("users_order_collection")
                    .whereEqualTo("orderStatus","PENDING")
                    .orderBy("orderPlacingTimeStamp")
                    .get()
                    .addOnCompleteListener {
                        if(it.isSuccessful){
                            for(document in it.result!!.documents){
                                val o = document.toObject(OrderItemMain::class.java)!!
                                o.docID = document.id
                                ordersMainArrayList.add(o)
                            }
                            if(ordersMainArrayList.isNotEmpty()){
                                placeOrderMainData(view)
                            }else{
                                view.noProductsText.visibility = View.VISIBLE
                                view.noProductsTextView.text = getString(R.string.you_have_no_orders)
                                view.progressBar.visibility = View.GONE
                                view.recyclerView.visibility = View.GONE
                                view.radioGroup.visibility = View.GONE
                            }
                        }else{
                            view.noProductsText.visibility = View.VISIBLE
                            view.noProductsTextView.text = getString(R.string.you_have_no_orders)
                            view.progressBar.visibility = View.GONE
                            view.recyclerView.visibility = View.GONE
                            view.radioGroup.visibility = View.GONE
                            it.exception!!.printStackTrace()
                        }
                    }
        }
    }

    private fun placeOrderMainData(view: View) {
        ordersMainHashMap.clear()
        ordersMainOldItemsArrayList.clear()
        for(order in ordersMainArrayList){
            val date = getDate(order.orderPlacingTimeStamp, "dd-MM-yyyy")
            if(ordersMainHashMap.containsKey(date)){
                ordersMainHashMap[date]!!.add(order)
            }else{
                ordersMainHashMap[date!!] = ArrayList()
                ordersMainHashMap[date]!!.add(order)
            }
        }
        for(item in ordersMainHashMap.entries){
            ordersMainOldItemsArrayList.add(
                    OrderOldItems(
                            date = item.key,
                            orders = item.value
                    )
            )
        }
        Collections.sort(ordersMainOldItemsArrayList, kotlin.Comparator { o1, o2 ->
            o1.orders[0].orderPlacingTimeStamp.compareTo(o2.orders[0].orderPlacingTimeStamp)
        })
        ordersMainOldItemsArrayList.reverse()
        view.recyclerView.layoutManager = LinearLayoutManager(view.context)
        view.recyclerView.adapter = OrderOldMainItemRecyclerAdapter(view.context, ordersMainOldItemsArrayList)

        view.noProductsText.visibility = View.GONE
        view.progressBar.visibility = View.GONE
        view.recyclerView.visibility = View.VISIBLE
        view.radioGroup.visibility = View.VISIBLE
        enableRadioGroupLogic(view)
    }

    private fun enableRadioGroupLogic(view: View) {
        view.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when(checkedId){
                R.id.pendingRadio -> {
                    loadSecondData(view, "PENDING")
                }
                R.id.approvedRadio -> {
                    loadSecondData(view, "APPROVED")
                }
                R.id.onDeliveryRadio -> {
                    loadSecondData(view, "DELIVERY")
                }
                R.id.completedRadio -> {
                    loadSecondData(view, "COMPLETED")
                }
            }
        }
    }

    private fun loadSecondData(view: View, s: String) {
        view.noProductsText.visibility = View.GONE
        view.progressBar.visibility = View.VISIBLE
        view.recyclerView.visibility = View.GONE
        view.radioGroup.visibility = View.VISIBLE
        firebaseFirestore.collection("users")
                .document(FirebaseAuth.getInstance().currentUser!!.uid)
                .collection("users_order_collection")
                .whereEqualTo("orderStatus",s)
                .orderBy("orderPlacingTimeStamp")
                .get()
                .addOnCompleteListener {
                    if(it.isSuccessful){
                        ordersMainArrayList.clear()
                        for(document in it.result!!.documents){
                            val o = document.toObject(OrderItemMain::class.java)!!
                            o.docID = document.id
                            ordersMainArrayList.add(o)
                        }
                        if(ordersMainArrayList.isNotEmpty()){
                            placeOrderMainData(view)
                        }else{
                            view.noProductsText.visibility = View.VISIBLE
                            view.noProductsTextView.text = getString(R.string.you_have_no_orders)
                            view.progressBar.visibility = View.GONE
                            view.recyclerView.visibility = View.GONE
                            view.radioGroup.visibility = View.VISIBLE
                        }
                    }else{
                        view.noProductsText.visibility = View.VISIBLE
                        view.noProductsTextView.text = getString(R.string.you_have_no_orders)
                        view.progressBar.visibility = View.GONE
                        view.recyclerView.visibility = View.GONE
                        view.radioGroup.visibility = View.VISIBLE
                        it.exception!!.printStackTrace()
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

    override fun onResume() {
        super.onResume()
        if(FirebaseAuth.getInstance().currentUser!=null){
            loadSecondData(mainView, "PENDING")
        }
    }
}