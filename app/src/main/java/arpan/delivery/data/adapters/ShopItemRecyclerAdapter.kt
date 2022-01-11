package arpan.delivery.data.adapters

import arpan.delivery.utils.Constants
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import arpan.delivery.R
import arpan.delivery.data.models.ShopItem
import arpan.delivery.ui.home.HomeActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.shop_item_view.view.*

class ShopItemRecyclerAdapter(
    private val context : Context,
    private val shopItems : ArrayList<ShopItem>,
    private val dataLocation : String) : RecyclerView.Adapter<ShopItemRecyclerAdapter.RecyclerViewHolder>() {

    class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView = itemView.shopImageItem as ImageView
        val textView = itemView.titleTextView as TextView
        val locationTextView = itemView.locationTextView as TextView
        val priceTextView = itemView.priceTextView as TextView
        val cardView = itemView.mainCardView as CardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val view = LayoutInflater.from(context).inflate(
            R.layout.shop_item_view, parent,
            false)
        return RecyclerViewHolder(
            view
        )
    }

    override fun getItemCount(): Int {
        return shopItems.size
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        holder.textView.text = shopItems[position].name
        holder.locationTextView.text = shopItems[position].location
        holder.priceTextView.text = "৳"+shopItems[position].deliver_charge

        if(shopItems[position].image != ""){
            val storageReference = FirebaseStorage.getInstance()
                    .getReference(Constants.FS_SHOPS_MAIN)
                    .child(shopItems[position].key)
                    .child(shopItems[position].image)

            Glide.with(context)
                    .load(storageReference)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .override(300,300)
                    .placeholder(R.drawable.loading_image_glide)
                    .into(holder.imageView)
        }

        holder.cardView.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("shop_key",shopItems[position].key)
            bundle.putString("shop_name",shopItems[position].name)
            bundle.putString("shop_location",shopItems[position].location)
            bundle.putString("cover_image",shopItems[position].cover_image)
            bundle.putString("image",shopItems[position].image)
            bundle.putString("deliver_charge",shopItems[position].deliver_charge)
            bundle.putString("da_charge",shopItems[position].da_charge)
            bundle.putString("shopNotice",shopItems[position].shopNotice)
            bundle.putString("shopNoticeColor",shopItems[position].shopNoticeColor)
            bundle.putString("shopNoticeColorBg",shopItems[position].shopNoticeColorBg)
            bundle.putBoolean("shopDiscount",shopItems[position].shopDiscount)
            bundle.putBoolean("shopCategoryDiscount",shopItems[position].shopCategoryDiscount)
            bundle.putString("shopCategoryDiscountName",shopItems[position].shopCategoryDiscountName)
            bundle.putFloat("shopDiscountPercentage",shopItems[position].shopDiscountPercentage)
            bundle.putFloat("shopDiscountMinimumPrice",shopItems[position].shopDiscountMinimumPrice)
            (context as HomeActivity).navController.navigate(R.id.productsFragment, bundle)
        }

    }



}