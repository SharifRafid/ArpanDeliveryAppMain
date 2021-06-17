package arpan.delivery.ui.cart

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arpan.delivery.data.adapters.CartProductItemRecyclerAdapter
import arpan.delivery.data.db.CartItemsRepo
import arpan.delivery.data.db.CartProductEntity
import com.shashank.sony.fancytoastlib.FancyToast
import kotlinx.coroutines.launch
import java.util.*

class CartViewModel(
    private val repository : CartItemsRepo) : ViewModel(){

    val cartItems = repository.carItems

    fun insertItemToCart(context : Context, cartProductEntity: CartProductEntity){
        viewModelScope.launch {
            val rowNum = repository.addCartItem(cartProductEntity)
            if(rowNum>-1){
                Log.e("CartViewModel", "Product Add Success")
            }else{
                Log.e("CartViewModel", "Product Add Failed")
            }
        }
    }
    fun updateItemToCart(context: Context, cartProductEntity: CartProductEntity){
        viewModelScope.launch {
            val rowNum = repository.update(cartProductEntity)
            if(rowNum>0){
                Log.e("CartViewModel", "Product Update Success")
            }else{
                Log.e("CartViewModel", "Product Update Failed")
            }
        }
    }
    fun updateItemToCartFromRecycler(context: Context, cartProductEntity: CartProductEntity){
        viewModelScope.launch {
            repository.update(cartProductEntity)
        }
    }
    fun deleteCartDataItem(context: Context, cartProductEntity: CartProductEntity){
        viewModelScope.launch {
            repository.deleteCartItem(cartProductEntity)
            Log.e("CartViewModel", "Product Deleted [CALLED]")
        }
    }
    fun deleteAll(){
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

}