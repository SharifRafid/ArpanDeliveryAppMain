package arpan.delivery.data.models

import androidx.annotation.Keep

@Keep
data class ProductItem (
        var key : String = "",
        var name : String = "",
        var shopKey : String = "",
        var shopCategoryId : String = "",
        var shopCategoryKey : String = "",
        var price : String = "",
        var image1 : String = "",
        var image2 : String = "",
        var image3 : String = "",
        var offerPrice : String = "",
        var offerStatus : String = "",
        var inStock : String = "",
        var description : String = "",
        var shortDescription : String = "",
        var offerDetails : String = "",
        var productDetails : String = "",
        var order : String = "",
        var arpanCharge : Int = 0,
)