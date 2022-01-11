package arpan.delivery.data.models

data class LocationItem(
    var key : String = "",
    var locationName : String = "",
    var deliveryCharge : Int = 0,
    var deliveryChargeClient : Int = 0,
    var daCharge : Int = 0,
    var order : Int = 0
)