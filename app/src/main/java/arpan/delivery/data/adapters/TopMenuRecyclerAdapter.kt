package arpan.delivery.data.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.RecyclerView
import arpan.delivery.R
import arpan.delivery.ui.home.HomeActivity
import kotlinx.android.synthetic.main.item_custom_top_view.view.*

class TopMenuRecyclerAdapter(val context : Context,
                      val images : List<Int>,
                      val titles : List<String>)
    : RecyclerView.Adapter<TopMenuRecyclerAdapter.RecyclerViewHolder>(){

    class RecyclerViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        val textView = itemView.title_text_view
        val imageView = itemView.image_view
        val cardView = itemView.cardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        return RecyclerViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.item_custom_top_view,parent,false))
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        holder.imageView.setImageResource(images[position])
        holder.textView.text = titles[position]
        holder.cardView.setOnClickListener {
            when(position){
                0->{(context as HomeActivity).navController.navigate(R.id.action_homeFragment_to_customOrderFragment)}
                1->{(context as HomeActivity).navController.navigate(R.id.action_homeFragment_to_medicineFragment)}
                2->{(context as HomeActivity).navController.navigate(R.id.action_homeFragment_to_parcelFragment)}
                3->{(context as HomeActivity).navController.navigate(R.id.action_homeFragment_to_pickUpDropFragment)}
            }
        }
    }

    override fun getItemCount(): Int {
        return titles.size
    }
}