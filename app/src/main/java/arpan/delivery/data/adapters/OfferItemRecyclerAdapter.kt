package arpan.delivery.data.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import arpan.delivery.R
import arpan.delivery.data.models.OfferImage
import arpan.delivery.utils.Constants
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.offers_item_view.view.*

class OfferItemRecyclerAdapter(
        private val context: Context,
        private val images: ArrayList<OfferImage>) : RecyclerView.Adapter
<OfferItemRecyclerAdapter.RecyclerViewHolder>() {

    class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView = itemView.imageView as ImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val view = LayoutInflater.from(context).inflate(
                R.layout.offers_item_view, parent,
                false)
        return RecyclerViewHolder(view)
    }

    override fun getItemCount(): Int {
        return images.size
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {

        val storageReference = FirebaseStorage.getInstance().getReference(Constants.FS_OFFERS_OI)
                .child(images[position].imageLocation)

        Glide.with(context)
                .load(storageReference)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imageView)

    }



}