package arpan.delivery.data.models

data class DaAgent(
    var key : String = "",
    var da_uid : String = "",
    var da_name : String = "",
    var da_mobile : String = "",
    var da_bkash : String = "",
    var da_password : String = "",
    var da_blood_group : String = "",
    var da_category : String = "",
    var da_image : String = "",
    var da_address : String = "",
    var da_status_active : Boolean = false,
)
