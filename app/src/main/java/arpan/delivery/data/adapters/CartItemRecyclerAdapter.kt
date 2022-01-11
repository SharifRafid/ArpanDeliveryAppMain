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
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import arpan.delivery.R
import arpan.delivery.data.db.CartProductEntity
import arpan.delivery.ui.home.HomeActivity
import arpan.delivery.utils.showToast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.card.MaterialCardView
import com.google.firebase.storage.FirebaseStorage
import com.shashank.sony.fancytoastlib.FancyToast
import kotlinx.android.synthetic.main.cart_main_item_view.view.*
import kotlinx.android.synthetic.main.cart_main_item_view.view.titleTextView
import kotlinx.android.synthetic.main.dialog_alert_layout_main.view.*

class CartItemRecyclerAdapter(
        private val context: Context,
        private val productItems: ArrayList<CartProductEntity>,
        private val positionMainAdapter : Int,
        private val mainCartProductItemRecyclerAdapter: CartProductItemRecyclerAdapter
        ) : RecyclerView.Adapter
    <CartItemRecyclerAdapter.RecyclerViewHolder>() {

    class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView = itemView.shopImageItem as ImageView
        val minusImage = itemView.minusImage as ImageView
        val plusImage = itemView.plus_image as ImageView
        val deleteTextView = itemView.deleteTextView as TextView
        val textView = itemView.titleTextView as TextView
        val descTextView = itemView.descTextView as TextView
        val amountTextView = itemView.amountTextView as TextView
        val price = itemView.offerPriceTextView as TextView
        val cardView = itemView.mainCardView as CardView
        val imageCardView = itemView.materialCardView as MaterialCardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val view = LayoutInflater.from(context).inflate(
                R.layout.cart_main_item_view, parent,
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
                holder.price.text = context.getString(R.string.price_will_be_confirmed_via_phone_call)
                holder.price.visibility = View.GONE
                holder.minusImage.visibility = View.INVISIBLE
                holder.amountTextView.visibility = View.GONE
                holder.plusImage.visibility = View.GONE
                holder.price.textSize = 14f
                if(cartProductEntity.medicine_order_text.isEmpty()){
                    holder.textView.text = context.getString(R.string.medicine_order)
                }else{
                    holder.textView.text = cartProductEntity.medicine_order_text
                }
                if(cartProductEntity.medicine_order_text_2.isEmpty()){
                    holder.descTextView.text = context.getString(R.string.medicine_order)
                }else{
                    holder.descTextView.text = cartProductEntity.medicine_order_text_2
                }
                if(cartProductEntity.medicine_order_image.isEmpty()){
                    holder.imageCardView.visibility = View.GONE
                }else{
                    holder.imageCardView.visibility = View.VISIBLE
                    holder.imageView.setImageURI(Uri.parse(cartProductEntity.medicine_order_image))
                }
            }
            cartProductEntity.parcel_item ->{
                holder.price.text = context.getString(R.string.price_will_be_confirmed_via_phone_call)
                holder.price.visibility = View.INVISIBLE
                holder.price.textSize = 14f
                holder.minusImage.visibility = View.GONE
                holder.amountTextView.visibility = View.GONE
                holder.plusImage.visibility = View.GONE
                if(cartProductEntity.parcel_order_text.isEmpty()){
                    holder.textView.text = context.getString(R.string.parcel_order)
                }else{
                    holder.textView.text = cartProductEntity.parcel_order_text
                }
                if(cartProductEntity.parcel_order_text_2.isEmpty()){
                    holder.descTextView.text = context.getString(R.string.parcel_order)
                }else{
                    holder.descTextView.text = cartProductEntity.parcel_order_text_2
                }
                if(cartProductEntity.parcel_order_image.isEmpty()){
                    holder.imageCardView.visibility = View.GONE
                }else{
                    holder.imageCardView.visibility = View.VISIBLE
                    holder.imageView.setImageURI(Uri.parse(cartProductEntity.parcel_order_image))
                }
            }
            cartProductEntity.custom_order_item ->{
                holder.price.text = context.getString(R.string.price_will_be_confirmed_via_phone_call)
                holder.price.visibility = View.INVISIBLE
                holder.minusImage.visibility = View.GONE
                holder.amountTextView.visibility = View.GONE
                holder.price.textSize = 14f
                holder.plusImage.visibility = View.GONE
                holder.textView.text = context.getString(R.string.custom_order)
                if(cartProductEntity.custom_order_text.isEmpty()){
                    holder.descTextView.text = context.getString(R.string.custom_order)
                }else{
                    holder.descTextView.text = cartProductEntity.custom_order_text
                }
                if(cartProductEntity.custom_order_image.isEmpty()){
                    holder.imageCardView.visibility = View.GONE
                }else{
                    holder.imageCardView.visibility = View.VISIBLE
                    holder.imageView.setImageURI(Uri.parse(cartProductEntity.custom_order_image))
                }
            }
            else ->{
                //Product Item
                holder.textView.text = cartProductEntity.product_item_name
                holder.descTextView.text = cartProductEntity.product_item_desc
                holder.amountTextView.text = cartProductEntity.product_item_amount.toString()
                holder.price.text = "৳${cartProductEntity.product_item_price} x ${cartProductEntity.product_item_amount} = ৳${cartProductEntity.product_item_price*cartProductEntity.product_item_amount}"
                if(cartProductEntity.product_item_image.isEmpty()){
                    holder.imageCardView.visibility = View.GONE
                }else{
                    holder.imageCardView.visibility = View.VISIBLE
                    val storageReference = FirebaseStorage.getInstance().getReference("shops")
                            .child(cartProductEntity.product_item_shop_key)
                            .child(cartProductEntity.product_item_image)

                    Glide.with(context)
                            .load(storageReference)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .override(300,300)
                            .placeholder(R.drawable.loading_image_glide).into(holder.imageView)
                }
                holder.minusImage.setOnClickListener {
                    if(cartProductEntity.product_item_amount>1){
                        cartProductEntity.product_item_amount = cartProductEntity.product_item_amount - 1
                        holder.amountTextView.text = cartProductEntity.product_item_amount.toString()
                        holder.price.text = "৳${cartProductEntity.product_item_price} x ${cartProductEntity.product_item_amount} = ৳${cartProductEntity.product_item_price*cartProductEntity.product_item_amount}"
                        (context as HomeActivity).cartViewModel.updateItemToCart(context, cartProductEntity)
                    }
                }
                holder.plusImage.setOnClickListener {
                    cartProductEntity.product_item_amount = cartProductEntity.product_item_amount + 1
                    holder.amountTextView.text = cartProductEntity.product_item_amount.toString()
                    holder.price.text = "৳${cartProductEntity.product_item_price} x ${cartProductEntity.product_item_amount} = ৳${cartProductEntity.product_item_price*cartProductEntity.product_item_amount}"
                    (context as HomeActivity).cartViewModel.updateItemToCart(context, cartProductEntity)
                }
            }
        }

        holder.deleteTextView.setOnClickListener{
            val view = LayoutInflater.from(context)
                    .inflate(R.layout.dialog_alert_layout_main, null)
            val dialog = AlertDialog.Builder(context)
                    .setView(view).create()
            view.btnNoDialogAlertMain.text = context.getString(R.string.no)
            view.btnYesDialogAlertMain.text = context.getString(R.string.yes)
            view.titleTextView.text = context.getString(R.string.are_you_sure_to_delete)
            view.messageTextView.text = context.getString(R.string.you_are_about_to_delete_this_product_from_your_cart)
            view.btnNoDialogAlertMain.setOnClickListener {
                dialog.dismiss()
            }
            view.btnYesDialogAlertMain.setOnClickListener {
                productItems.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeRemoved(position, productItems.size)
                (context as HomeActivity).cartViewModel.deleteCartDataItem(context, cartProductEntity)
                if(productItems.isEmpty()){
                    mainCartProductItemRecyclerAdapter.productItems.removeAt(positionMainAdapter)
                    mainCartProductItemRecyclerAdapter.notifyItemRemoved(positionMainAdapter)
                    mainCartProductItemRecyclerAdapter.notifyItemRangeChanged(positionMainAdapter, mainCartProductItemRecyclerAdapter.productItems.size)
                }
                dialog.dismiss()
            }
            dialog.show()
        }
    }
}