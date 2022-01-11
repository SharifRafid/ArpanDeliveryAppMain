package arpan.delivery.utils

class Constants {
    companion object{

        const val MAIN_SHOPS_PAGING_ARRAYLIST_SIZE = 10

        //COUNTS RTDB LOCATION
        const val COUNT_PRODUCT_CATEGORY_MAIN = "product_counts_category_main"
        const val COUNT_SHOP_CATEGORY_MAIN = "shop_counts_category_main"
        const val COUNT_ALL_PRODUCT_MAIN = "product_counts_all_main"
        const val COUNT_ALL_SHOPS_MAIN = "shop_counts_all_main"

        const val OFFER_IMAGE_FIREBASE_STORAGE_DATA_LOCATION = "offer_images"
        const val OFFER_IMAGE_FIRESTORE_DATA_LOCATION = "offer_images"

        //FirebaseFirestore Offers Offer Image
        const val FS_OFFERS_OI = "offers"
        const val FC_OFFERS_OI = "offers"
        const val FD_OFFERS_OID = "offer_images_details"
        const val FD_OFFERS_OIS = "image_offer_status"
        const val FIELD_FD_OFFERS_OIS = "image_offer_status_field"
        const val FIELD_FD_OFFERS_OID_DESCRIPTION = "offer_image_description"
        const val FIELD_FD_OFFERS_OID_LOCATION = "offer_image_location_storage"
        const val FIELD_FD_OFFERS_OID_ORDER = "offer_image_order_value"

        //FirebaseFirestore Shops Main
        const val FC_SHOPS_MAIN = "shops_main"
        const val FS_SHOPS_MAIN = "shops"
        const val FIELD_FD_SM_COVER = "shop_cover_photo"
        const val FIELD_FD_SM_CATEGORY = "shop_main_category"
        const val FIELD_FD_SM_DA_CHARGE = "shop_main_da_charge"
        const val FIELD_FD_SM_DELIVERY = "shop_main_delivery_charge"
        const val FIELD_FD_SM_ICON = "shop_main_icon"
        const val FIELD_FD_SM_LOCATION = "shop_main_location"
        const val FIELD_FD_SM_NAME = "shop_main_name"
        const val FIELD_FD_SM_USERNAME = "shop_main_user_name"
        const val FIELD_FD_SM_PASSWORD = "shop_main_password"
        const val FIELD_FD_SM_ORDER = "shop_main_order"
        const val FIELD_FD_SM_STATUS = "shop_main_status"
        const val FIELD_FD_SM_IS_CLIENT = "shop_main_is_client"

        //FirebaseFirestore Shops Category Main
        const val FC_SHOPS_MAIN_CATEGORY = "shop_categories"
        const val FD_SHOPS_MAIN_CATEGORY = "shop_categories"
        const val FIELD_FD_SHOPS_MAIN_CATEGORY_KEY = "shop_category_key"
        const val FIELD_FD_SHOPS_MAIN_CATEGORY_NAME = "shop_category_name"
        const val FIELD_FD_SHOPS_MAIN_CATEGORY_ORDER = "shop_category_order"

        //FirebaseFirestore Products Category Main
        const val FD_PRODUCTS_MAIN_CATEGORY = "product_categories"
        const val FIELD_FD_PRODUCTS_MAIN_CATEGORY_KEY = "product_category_key"
        const val FIELD_FD_PRODUCTS_MAIN_CATEGORY_NAME = "product_category_name"
        const val FIELD_FD_PRODUCTS_MAIN_CATEGORY_ORDER = "product_category_order"


        //Firebase Firestore Products Location
        const val FD_PRODUCTS_MAIN_SUB_COLLECTION = "products_main_sub_collection"

    }
}