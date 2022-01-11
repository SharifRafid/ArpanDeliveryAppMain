package arpan.delivery.data.db

class CartItemsRepo(private val dao : CartInterface) {

    val carItems = dao.getAllCartItems()

    suspend fun addCartItem(cartProductEntity: CartProductEntity) : Long{
        return dao.addProductToCart(cartProductEntity)
    }

    suspend fun update(cartProductEntity: CartProductEntity) : Int{
        return dao.updateCartItem(cartProductEntity)
    }

    suspend fun deleteCartItem(cartProductEntity: CartProductEntity){
        return dao.deleteCartItem(cartProductEntity)
    }

    suspend fun deleteAll(){
        dao.deleteAll()
    }

}