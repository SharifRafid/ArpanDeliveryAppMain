package arpan.delivery.data.models

data class ShopItem (
    val key : String = "",
    val name : String = "",
    val categories : String = "",
    val image : String = "",
    val cover_image : String = "",
    val da_charge : String = "",
    val deliver_charge : String = "",
    val location : String = "",
    val username : String = "",
    val password : String = "",
    val order : Int = 0,
    val isClient : String = "no",
    var shopNotice : String = "",
    var shopNoticeColor : String = "",
    var shopNoticeColorBg : String = "",
    var status : String = "",

    var shopDiscount : Boolean = false,
    var shopCategoryDiscount : Boolean = false,
    var shopCategoryDiscountName : String = "",
    var shopDiscountPercentage : Float = 0f,
    var shopDiscountMinimumPrice : Float = 0f,
)