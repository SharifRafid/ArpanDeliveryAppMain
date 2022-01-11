package arpan.delivery.data.models

data class SlidingTextItem(
    var key : String = "",
    var enabled : Boolean = false,
    var textTitle : String = "",
    var textDescription : String = "",
    var timeBased : Boolean = false,
    var startTime : Long = 0,
    var endTime : Long = 0,
    var backgroundColorHex : String = "",
    var textColorHex : String = "",
    var order : Long = 0,
    var startTimeString : String = "00:00",
    var endTimeString : String = "00:00"
)