package arpan.delivery.data.models

import arpan.delivery.data.db.CartProductEntity

data class MainShopCartItem(
    var key : String = "",
    var shop_doc_id : String = "",
    var cart_products : ArrayList<CartProductEntity> = ArrayList(),
    var shop_details : ShopItem = ShopItem()
)