package arpan.delivery.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "cart_data_table")
data class CartProductEntity (

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "cart_id")
    var id : Int = 0,

    //Item Separation
    @ColumnInfo(name = "product_item")
    var product_item : Boolean = false,
    @ColumnInfo(name = "custom_order_item")
    var custom_order_item : Boolean = false,
    @ColumnInfo(name = "medicine_item")
    var medicine_item : Boolean = false,
    @ColumnInfo(name = "parcel_item")
    var parcel_item : Boolean = false,

    //Product Item
    @ColumnInfo(name = "product_item_key")
    var product_item_key : String = "",
    @ColumnInfo(name = "product_item_name")
    var product_item_name : String = "",
    @ColumnInfo(name = "product_item_shop_key")
    var product_item_shop_key : String = "",
    @ColumnInfo(name = "product_item_shop_name")
    var product_item_shop_name : String = "",
    @ColumnInfo(name = "product_item_category_tag")
    var product_item_category_tag : String = "",
    @ColumnInfo(name = "product_item_price")
    var product_item_price : Int = 0,
    @ColumnInfo(name = "product_item_offer_price")
    var product_item_offer_price : Int = 0,
    @ColumnInfo(name = "product_arpan_profit")
    var product_arpan_profit : Int = 0,
    @ColumnInfo(name = "product_item_image")
    var product_item_image : String = "",
    @ColumnInfo(name = "product_item_desc")
    var product_item_desc : String = "",
    @ColumnInfo(name = "product_item_amount")
    var product_item_amount : Int = 1,

    //Custom Order
    @ColumnInfo(name = "custom_order_text")
    var custom_order_text : String = "",
    @ColumnInfo(name = "custom_order_image")
    var custom_order_image : String = "",

    //Medicine Order
    @ColumnInfo(name = "medicine_order_text")
    var medicine_order_text : String = "",
    @ColumnInfo(name = "medicine_order_text_2")
    var medicine_order_text_2 : String = "",
    @ColumnInfo(name = "medicine_order_image")
    var medicine_order_image : String = "",

    //Parcel Order
    @ColumnInfo(name = "parcel_order_text")
    var parcel_order_text : String = "",
    @ColumnInfo(name = "parcel_order_text_2")
    var parcel_order_text_2 : String = "",
    @ColumnInfo(name = "parcel_order_image")
    var parcel_order_image : String = ""
)