package arpan.delivery.data.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import arpan.delivery.R
import arpan.delivery.data.db.CartProductEntity
import arpan.delivery.ui.home.HomeActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.card.MaterialCardView
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.order_item_view.view.*

class OrderItemRecyclerAdapter(
        private val context: Context,
        private val productItems: ArrayList<CartProductEntity>) : RecyclerView.Adapter
    <OrderItemRecyclerAdapter.RecyclerViewHolder>() {

    class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView = itemView.titleTextView as TextView
        val amountTextView = itemView.amountTextView as TextView
        val price = itemView.priceTextView as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val view = LayoutInflater.from(context).inflate(
                R.layout.order_item_view, parent,
            false)
        return RecyclerViewHolder(view)
    }

    override fun getItemCount(): Int {
        return productItems.size
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val cartProductEntity = productItems[position]
        when{
            cartProductEntity.medicine_item ->{
                holder.price.text = context.getString(R.string.price_0_taka)
                holder.textView.text = context.getString(R.string.medicine_order)
            }
            cartProductEntity.parcel_item ->{
                holder.price.text = context.getString(R.string.price_0_taka)
                holder.textView.text = context.getString(R.string.parcel_order)
            }
            cartProductEntity.custom_order_item ->{
                holder.price.text = context.getString(R.string.price_0_taka)
                holder.textView.text = context.getString(R.string.custom_order)
            }
            else ->{
                //Product Item
                holder.textView.text = cartProductEntity.product_item_name
                holder.amountTextView.text = cartProductEntity.product_item_amount.toString()
                holder.price.text = "৳ ${cartProductEntity.product_item_price*cartProductEntity.product_item_amount}"
            }
        }
    }
}