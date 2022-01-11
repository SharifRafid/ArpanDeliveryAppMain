package arpan.delivery.data.adapters

import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.RecyclerView
import arpan.delivery.R
import arpan.delivery.data.db.CartProductEntity
import arpan.delivery.data.models.ProductItem
import arpan.delivery.ui.cart.CartViewModel
import arpan.delivery.ui.home.HomeActivity
import arpan.delivery.utils.showToast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.card.MaterialCardView
import com.google.firebase.storage.FirebaseStorage
import com.lid.lib.LabelImageView
import com.shashank.sony.fancytoastlib.FancyToast
import kotlinx.android.synthetic.main.product_item_view.view.*
import kotlin.math.roundToInt

class ProductItemRecyclerAdapter(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val context: Context,
    private val activity: Activity,
    private val productItems: ArrayList<ProductItem>,
    private val shopName: String,
    private val categoryKey: String,
    private val shopKey: String,
    private val cartViewModel: CartViewModel,
    private val shopDiscount: Boolean,
    private val shopCategoryDiscount: Boolean,
    private val shopCategoryDiscountName: String,
    private val shopDiscountPercentage: Float,
    private val shopDiscountMinimumPrice: Float
) : RecyclerView.Adapter
    <ProductItemRecyclerAdapter.RecyclerViewHolder>() {

    class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView = itemView.shopImageItem as LabelImageView
        val addToCartImageView = itemView.addToCartTextView as ImageView
        val textView = itemView.titleTextView as TextView
        val descTextView = itemView.descTextView as TextView
        val price = itemView.priceTextView as TextView
        val offerPrice = itemView.offerPriceTextView as TextView
        val cardView = itemView.mainCardView as MaterialCardView
        val imageCardView = itemView.materialCardView as MaterialCardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val view = LayoutInflater.from(context).inflate(
                R.layout.product_item_view, parent,
            false)
        return RecyclerViewHolder(view)
    }

    override fun getItemCount(): Int {
        return productItems.size
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        holder.textView.text = productItems[position].name

        holder.descTextView.text = productItems[position].shortDescription

        if(productItems[position].price.toInt() != productItems[position].offerPrice.toInt()
                && productItems[position].offerPrice.toInt() != 0){
            holder.price.visibility = View.VISIBLE
            holder.price.text = " ${productItems[position].price.toInt()+productItems[position].arpanCharge}"
            holder.price.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            holder.offerPrice.text = "${productItems[position].offerPrice.toInt()+productItems[position].arpanCharge}"
        }else{
            if(shopDiscount){
                if(shopCategoryDiscount){
                    if(shopCategoryDiscountName==categoryKey){
                        if(productItems[position].price.toFloat()>=shopDiscountMinimumPrice){
                            holder.price.visibility = View.VISIBLE
                            holder.price.text = " ${productItems[position].price.toInt()+productItems[position].arpanCharge}"
                            holder.price.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                            holder.offerPrice.text = "${productItems[position].offerPrice.toInt()-roundNumberPriceTotal(productItems[position].offerPrice.toInt()*shopDiscountPercentage)+productItems[position].arpanCharge}"
                        }else{
                            holder.price.visibility = View.GONE
                            holder.offerPrice.text = "${productItems[position].offerPrice.toInt()+productItems[position].arpanCharge}"
                        }
                    }else{
                        holder.price.visibility = View.GONE
                        holder.offerPrice.text = "${productItems[position].offerPrice.toInt()+productItems[position].arpanCharge}"
                    }
                }else{
                    if(productItems[position].price.toFloat()>=shopDiscountMinimumPrice){
                        holder.price.visibility = View.VISIBLE
                        holder.price.text = " ${productItems[position].price.toInt()+productItems[position].arpanCharge}"
                        holder.price.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                        holder.offerPrice.text = "${productItems[position].offerPrice.toInt()-roundNumberPriceTotal(productItems[position].offerPrice.toInt()*shopDiscountPercentage)+productItems[position].arpanCharge}"
                    }else{
                        holder.price.visibility = View.GONE
                        holder.offerPrice.text = "${productItems[position].offerPrice.toInt()+productItems[position].arpanCharge}"
                    }
                }
            }else{
                holder.price.visibility = View.GONE
                holder.offerPrice.text = "${productItems[position].offerPrice.toInt()+productItems[position].arpanCharge}"
            }
        }

        holder.imageView.isLabelVisual = productItems[position].offerStatus=="active"

        if(productItems[position].image1.isNotEmpty()){
            val storageReference = FirebaseStorage.getInstance().getReference("shops")
                .child(shopKey)
                .child(productItems[position].image1)

            Glide.with(context)
                .load(storageReference)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .placeholder(R.drawable.loading_image_glide)
                .into(holder.imageView)

            holder.imageCardView.visibility = View.VISIBLE
        }else{
            holder.imageCardView.visibility = View.GONE
        }

        holder.addToCartImageView.setOnClickListener {
            var updateIndex = 0
            var updateAmount = 0
            var updateMode = false
            val array = (activity as HomeActivity).cartItemsAllMainList
            for(a in array){
                if(a.product_item_key.isNotEmpty()){
                    if(a.product_item_key == productItems[position].key){
                        updateIndex = a.id
                        updateAmount = a.product_item_amount
                        updateMode = true
                        break
                    }else{
                        updateMode = false
                    }
                }
            }
            if(!updateMode){
                if((context as HomeActivity).sets.contains(productItems[position].shopKey)){
                    val totalPrice = if(shopDiscount){
                        if(shopCategoryDiscount){
                            if(shopCategoryDiscountName==categoryKey){
                                if(productItems[position].price.toFloat()>=shopDiscountMinimumPrice){
                                    productItems[position].offerPrice.toInt()-roundNumberPriceTotal(productItems[position].offerPrice.toInt()*shopDiscountPercentage)+productItems[position].arpanCharge
                                }else{
                                    //Break and Do as   was
                                    productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                                }
                            }else{
                                //Break and Do as   was
                                productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                            }
                        }else{
                            if(productItems[position].price.toFloat()>=shopDiscountMinimumPrice){
                                productItems[position].offerPrice.toInt()-roundNumberPriceTotal(productItems[position].offerPrice.toInt()*shopDiscountPercentage)+productItems[position].arpanCharge
                            }else{
                                //Break and Do as   was
                                productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                            }
                        }
                    }else{
                        //Break and Do as   was
                        productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                    }
                    cartViewModel.insertItemToCart(context,
                            CartProductEntity(
                                    product_item = true,
                                    product_item_key = productItems[position].key,
                                    product_item_name = productItems[position].name,
                                    product_item_shop_key= productItems[position].shopKey,
                                    product_item_category_tag = productItems[position].shopCategoryKey,
                                    product_arpan_profit =  productItems[position].arpanCharge,
                                    product_item_price = totalPrice,
                                    product_item_image = productItems[position].image1,
                                    product_item_desc = productItems[position].shortDescription,
                                    product_item_amount = 1
                            )
                    )
                    //context.showToast( context.getString(R.string.product_added_to_cart),FancyToast.SUCCESS)
                    if(!(context as HomeActivity).popUpForCartRedirect.isShowing){
                        context.showPopUpWindowForCart()
                    }
                }else{
                    if((context as HomeActivity).sets.size < (context as HomeActivity).homeViewModel.getMaxShops().value!!.toInt()){
                        val totalPrice = if(shopDiscount){
                            if(shopCategoryDiscount){
                                if(shopCategoryDiscountName==categoryKey){
                                    if(productItems[position].price.toFloat()>=shopDiscountMinimumPrice){
                                        productItems[position].offerPrice.toInt()-roundNumberPriceTotal(productItems[position].offerPrice.toInt()*shopDiscountPercentage)+productItems[position].arpanCharge
                                    }else{
                                        //Break and Do as   was
                                        productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                                    }
                                }else{
                                    //Break and Do as   was
                                    productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                                }
                            }else{
                                if(productItems[position].price.toFloat()>=shopDiscountMinimumPrice){
                                    productItems[position].offerPrice.toInt()-roundNumberPriceTotal(productItems[position].offerPrice.toInt()*shopDiscountPercentage)+productItems[position].arpanCharge
                                }else{
                                    //Break and Do as   was
                                    productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                                }
                            }
                        }else{
                            //Break and Do as   was
                            productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                        }
                        cartViewModel.insertItemToCart(context,
                                CartProductEntity(
                                        product_item = true,
                                        product_item_key = productItems[position].key,
                                        product_item_name = productItems[position].name,
                                        product_item_shop_key= productItems[position].shopKey,
                                        product_item_category_tag = productItems[position].shopCategoryKey,
                                        product_arpan_profit =  productItems[position].arpanCharge,
                                        product_item_price = totalPrice,
                                        product_item_image = productItems[position].image1,
                                        product_item_desc = productItems[position].shortDescription,
                                        product_item_amount = 1
                                )
                        )
                        //context.showToast(context.getString(R.string.product_added_to_cart),FancyToast.SUCCESS )
                        if(!(context as HomeActivity).popUpForCartRedirect.isShowing){
                            context.showPopUpWindowForCart()
                        }
                    }else{
                        if((context as HomeActivity).homeViewModel.getAllowMoreShops().value!!){
                            val totalPrice = if(shopDiscount){
                                if(shopCategoryDiscount){
                                    if(shopCategoryDiscountName==categoryKey){
                                        if(productItems[position].price.toFloat()>=shopDiscountMinimumPrice){
                                            productItems[position].offerPrice.toInt()-roundNumberPriceTotal(productItems[position].offerPrice.toInt()*shopDiscountPercentage)+productItems[position].arpanCharge
                                        }else{
                                            //Break and Do as   was
                                            productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                                        }
                                    }else{
                                        //Break and Do as   was
                                        productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                                    }
                                }else{
                                    if(productItems[position].price.toFloat()>=shopDiscountMinimumPrice){
                                        productItems[position].offerPrice.toInt()-roundNumberPriceTotal(productItems[position].offerPrice.toInt()*shopDiscountPercentage)+productItems[position].arpanCharge
                                    }else{
                                        //Break and Do as   was
                                        productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                                    }
                                }
                            }else{
                                //Break and Do as   was
                                productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                            }
                            cartViewModel.insertItemToCart(context,
                                    CartProductEntity(
                                            product_item = true,
                                            product_item_key = productItems[position].key,
                                            product_item_name = productItems[position].name,
                                            product_item_shop_key= productItems[position].shopKey,
                                            product_item_category_tag = productItems[position].shopCategoryKey,
                                            product_item_price = totalPrice,
                                            product_arpan_profit =  productItems[position].arpanCharge,
                                            product_item_image = productItems[position].image1,
                                            product_item_desc = productItems[position].shortDescription,
                                            product_item_amount = 1
                                    )
                            )
                            //context.showToast( context.getString(R.string.product_added_to_cart),FancyToast.SUCCESS )
                            if(!(context as HomeActivity).popUpForCartRedirect.isShowing){
                                context.showPopUpWindowForCart()
                            }
                        }else{
                            context.showToast( context.getString(R.string.reached_max_order_limit),FancyToast.ERROR )
                        }
                    }
                }
            }else{
                val totalPrice = if(shopDiscount){
                    if(shopCategoryDiscount){
                        if(shopCategoryDiscountName==categoryKey){
                            if(productItems[position].price.toFloat()>=shopDiscountMinimumPrice){
                                productItems[position].offerPrice.toInt()-roundNumberPriceTotal(productItems[position].offerPrice.toInt()*shopDiscountPercentage)+productItems[position].arpanCharge
                            }else{
                                //Break and Do as   was
                                productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                            }
                        }else{
                            //Break and Do as   was
                            productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                        }
                    }else{
                        if(productItems[position].price.toFloat()>=shopDiscountMinimumPrice){
                            productItems[position].offerPrice.toInt()-roundNumberPriceTotal(productItems[position].offerPrice.toInt()*shopDiscountPercentage)+productItems[position].arpanCharge
                        }else{
                            //Break and Do as   was
                            productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                        }
                    }
                }else{
                    //Break and Do as   was
                    productItems[position].offerPrice.toInt()+productItems[position].arpanCharge
                }
                cartViewModel.updateItemToCart(context,
                        CartProductEntity(
                                id = updateIndex,
                                product_item = true,
                                product_item_key = productItems[position].key,
                                product_item_name = productItems[position].name,
                                product_item_shop_key= productItems[position].shopKey,
                                product_item_category_tag = productItems[position].shopCategoryKey,
                                product_item_price = totalPrice,
                                product_arpan_profit =  productItems[position].arpanCharge,
                                product_item_image = productItems[position].image1,
                                product_item_desc = productItems[position].shortDescription,
                                product_item_amount = updateAmount+1
                        )
                )
                //context.showToast(context.getString(R.string.cart_updated_successfully),FancyToast.SUCCESS)
                if(!(context as HomeActivity).popUpForCartRedirect.isShowing){
                    context.showPopUpWindowForCart()
                }
            }
        }
    }

    private fun roundNumberPriceTotal(d: Float): Int {
        //This  is a special round function exclusively for this  page of the app
        //not usable for general parts and other parts of   the code or apps
        return d.toInt()
    }
}