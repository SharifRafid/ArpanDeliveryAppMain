package arpan.delivery.data.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arpan.delivery.R
import arpan.delivery.data.models.MainShopCartItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.cart_product_item_view.view.*

class CartProductItemRecyclerAdapter(
        private val context : Context,
        val productItems : ArrayList<MainShopCartItem>
) : RecyclerView.Adapter
    <CartProductItemRecyclerAdapter.RecyclerViewHolder>() {

    private var firebaseFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var cartItemRecyclerAdapter: CartItemRecyclerAdapter

    class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productsTextView = itemView.productsTextView as TextView
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
        cartItemRecyclerAdapter = CartItemRecyclerAdapter(context, productItems[position].cart_products, position, this)
        holder.productsTextView.text = productItems[position].shop_details.name
        holder.productsRecyclerView.layoutManager = LinearLayoutManager(context)
        holder.productsRecyclerView.adapter = cartItemRecyclerAdapter
    }
}