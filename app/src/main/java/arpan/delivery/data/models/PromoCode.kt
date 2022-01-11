package arpan.delivery.data.models

data class PromoCode(
    var key : String = "",
    var startDate : Long = 0,
    var minimumPrice : Int = 0,
    var discountPrice : Int = 0,
    var maxUses : Int = 0,
    var remainingUses : Int = 0,
    var promoCodeName : String = "",
    var validityOfCode : Long = 0,
    var active : Boolean = false,

    var shopBased : Boolean = false,
    var shopKey : String = "",
    var shopName : String = "",

    var shopDiscount : Boolean = false,

    var deliveryDiscount : Boolean = false,

    var bothDiscount : Boolean = false,

    var onceForOneUser : Boolean = false,
    var userIds : String = "",


)