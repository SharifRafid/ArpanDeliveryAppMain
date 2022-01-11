package arpan.delivery.data.models

data class OrderOldItems(
    var key : String = "",
    var date : String = "",
    var orders : ArrayList<OrderItemMain> = ArrayList()
)