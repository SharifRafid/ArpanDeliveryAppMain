package arpan.delivery.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CartInterface {

    @Insert
    suspend fun addProductToCart(cartProductEntity: CartProductEntity) : Long

    @Query("SELECT * FROM cart_data_table ORDER BY cart_id DESC")
    fun getAllCartItems() : LiveData<List<CartProductEntity>>

    @Delete
    suspend fun deleteCartItem(cartProductEntity: CartProductEntity)

    @Update
    suspend fun updateCartItem(cartProductEntity: CartProductEntity) : Int

    @Query("DELETE FROM cart_data_table")
    suspend fun deleteAll()
}