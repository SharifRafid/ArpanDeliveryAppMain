package arpan.delivery.data.models

import arpan.delivery.data.db.CartProductEntity

data class OrderItemMain(
    var key : String = "",
    var docID : String = "",
    var userId : String = "",
    var userPhoneAccount : String = "",
    var userName : String = "",
    var userNumber : String = "",
    var userNote : String = "",
    var userAddress : String = "",
    var products : List<CartProductEntity> = ArrayList(),
    var promoCodeApplied : Boolean = false,
    var promoCode: PromoCode = PromoCode(),
    var paymentMethod : String = "",
    var totalPrice : Int = 0,
    var locationItem: LocationItem = LocationItem(),
    var deliveryCharge : Int = 0,
    var orderPlacingTimeStamp : Long = 0,
    var lastTouchedTimeStamp : Long = 0,
    var orderStatus : String = "PENDING",
    var paid : Boolean = false,
    var orderId : String = "",
    var pickDropOrder : Boolean = false,
    var pickDropOrderItem : PickDropOrderItem = PickDropOrderItem(),
    var lattitude : String = "",
    var longtitude : String = "",
    var orderCompletedStatus : String = "",
    var daCharge : Int = 0,
    var daDetails : DaAgent = DaAgent(),

    var verifiedTimeStampMillis : Long = 0,
    var processingTimeStampMillis : Long = 0,
    var pickedUpTimeStampMillis : Long = 0,
    var completedTimeStampMillis : Long = 0,

    var cancelledOrderReasonFromAdmin : String = "",

    var adminOrderNote : String = "",

    var paymentRequested : Boolean = false,
    var paymentCompleted : Boolean = false,
)