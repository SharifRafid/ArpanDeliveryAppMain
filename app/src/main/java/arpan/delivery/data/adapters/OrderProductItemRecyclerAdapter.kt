package arpan.delivery.data.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arpan.delivery.R
import arpan.delivery.data.models.MainShopCartItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.cart_product_item_view.view.*

class OrderProductItemRecyclerAdapter(
        private val context : Context,
        private val productItems : ArrayList<MainShopCartItem>
) : RecyclerView.Adapter
    <OrderProductItemRecyclerAdapter.RecyclerViewHolder>() {

    private var firebaseFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var cartItemRecyclerAdapter: OrderItemRecyclerAdapter

    class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productsTextView = itemView.productsTextView as TextView
        val priceTextView = itemView.priceTextView as TextView
        val productsRecyclerView = itemView.productsRecyclerView as RecyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val view = LayoutInflater.from(context).inflate(
                R.layout.cart_product_item_view, parent,
            false)
        return RecyclerViewHolder(view)
    }

    override fun getItemCount(): Int {
        return productItems.size
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        holder.priceTextView.visibility = View.VISIBLE
        cartItemRecyclerAdapter = OrderItemRecyclerAdapter(context, productItems[position].cart_products)
        holder.productsTextView.text = productItems[position].shop_details.name
        holder.productsRecyclerView.layoutManager = LinearLayoutManager(context)
        holder.productsRecyclerView.adapter = cartItemRecyclerAdapter
        var chargeTotal = 0
        productItems[position].cart_products.forEach {
            chargeTotal += (it.product_item_price * it.product_item_amount)
        }
        holder.priceTextView.text = context.getString(R.string.total)+" "+chargeTotal+"৳"
    }
}