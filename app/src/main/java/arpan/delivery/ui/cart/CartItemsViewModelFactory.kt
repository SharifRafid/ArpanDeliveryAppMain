package arpan.delivery.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import arpan.delivery.data.db.CartItemsRepo
import java.lang.IllegalArgumentException

class CartItemsViewModelFactory(private val repository: CartItemsRepo):ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(CartViewModel::class.java)){
            return CartViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown View Model Class")
    }
}