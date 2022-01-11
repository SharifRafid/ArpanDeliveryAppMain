package arpan.delivery.data.adapters

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arpan.delivery.R
import arpan.delivery.data.models.MainShopCartItem
import arpan.delivery.data.models.OrderItemMain
import arpan.delivery.data.models.OrderOldItems
import arpan.delivery.ui.home.HomeActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_order_history.view.*
import kotlinx.android.synthetic.main.old_orders_list_view.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class OrderOldSubItemRecyclerAdapter(
        private val context : Context,
        private val productItems : ArrayList<OrderItemMain>
) : RecyclerView.Adapter
    <OrderOldSubItemRecyclerAdapter.RecyclerViewHolder>() {

    private var firebaseFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var cartItemRecyclerAdapter: OrderItemRecyclerAdapter

    class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderIdTextView = itemView.textView as TextView
        val timeTextView = itemView.time as TextView
        val statusTextView = itemView.status as TextView
        val cardView = itemView.cardView as CardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val view = LayoutInflater.from(context).inflate(
                R.layout.old_orders_list_view, parent,
            false)
        return RecyclerViewHolder(view)
    }

    override fun getItemCount(): Int {
        return productItems.size
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        holder.orderIdTextView.text = productItems[position].orderId
        holder.timeTextView.text = getDate(productItems[position].orderPlacingTimeStamp,"hh:mm a")
        if(productItems[position].orderCompletedStatus == "CANCELLED") {
            holder.statusTextView.text = "CANCELLED"
            holder.statusTextView.setBackgroundColor(Color.parseColor("#EA594D"))
        }else{
            holder.statusTextView.text = productItems[position].orderStatus
            when(productItems[position].orderStatus){
                "PENDING" -> holder.statusTextView.setBackgroundColor(Color.parseColor("#262626"))
                "VERIFIED" -> holder.statusTextView.setBackgroundColor(Color.parseColor("#FA831B"))
                "PROCESSING" -> holder.statusTextView.setBackgroundColor(Color.parseColor("#ED9D34"))
                "PICKED UP" -> holder.statusTextView.setBackgroundColor(Color.parseColor("#ED9D34"))
                "COMPLETED" -> holder.statusTextView.setBackgroundColor(Color.parseColor("#43A047"))
            }
        }
        holder.cardView.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("orderID",productItems[position].docID)
            (context as HomeActivity).navController.navigate(R.id.orderHistoryFragment, bundle)
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
}