package arpan.delivery.data.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.RecyclerView
import arpan.delivery.R
import arpan.delivery.ui.home.HomeActivity
import arpan.delivery.utils.showToast
import com.google.firebase.auth.FirebaseAuth
import com.shashank.sony.fancytoastlib.FancyToast
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
            if(!(context as HomeActivity).completeCountOfListeners.contains(false)){
                when(position){
                    0->{
                        if(FirebaseAuth.getInstance().currentUser!=null){
                            (context as HomeActivity).navController.navigate(R.id.customOrderNewFragment)
                        }else{
                            context.showToast("অর্ডার করার জন্য লগ ইন করতে হবে ।", FancyToast.ERROR)
                        }
                    }
                    1-> {
                        if(FirebaseAuth.getInstance().currentUser!=null){
                            (context as HomeActivity).navController.navigate(R.id.medicineNewFragment)
                        }else{
                            context.showToast("অর্ডার করার জন্য লগ ইন করতে হবে ।", FancyToast.ERROR)
                        }
                    }
                    2->{
                        if(FirebaseAuth.getInstance().currentUser!=null){
                            (context as HomeActivity).navController.navigate(R.id.parcelNewFragment)
                        }else{
                            context.showToast("অর্ডার করার জন্য লগ ইন করতে হবে ।", FancyToast.ERROR)
                        }
                    }
                    3->{
                        if(FirebaseAuth.getInstance().currentUser!=null){
                            (context as HomeActivity).navController.navigate(R.id.pickUpDropFragment)
                        }else{
                            context.showToast("অর্ডার করার জন্য লগ ইন করতে হবে ।", FancyToast.ERROR)
                        }
                    }
                }
            }else{
                context.showToast("একটু অপেক্ষা করুন", FancyToast.LENGTH_SHORT)
            }
        }
    }

    override fun getItemCount(): Int {
        return titles.size
    }
}