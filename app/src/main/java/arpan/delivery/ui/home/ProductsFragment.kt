package arpan.delivery.ui.home

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import arpan.delivery.R
import arpan.delivery.data.adapters.ViewPagerAdapterProducts
import arpan.delivery.data.models.ProductCategoryItem
import arpan.delivery.utils.Constants
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.fragment_products.view.*
import java.util.*
import kotlin.collections.ArrayList

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val shop_key = "shop_key"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProductsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProductsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var shop_key: String? = null
    private var param2: String? = null

    private var shop_name : String? = null
    private var shop_location : String? = null
    private var cover_image : String? = null
    private var image : String? = null
    private var deliver_charge : String? = null
    private var da_charge : String? = null
    private var shopNotice : String? = null
    private var shopNoticeColor : String? = null
    private var shopNoticeColorBg : String? = null

    private var shopDiscount : Boolean = false
    private var shopCategoryDiscount : Boolean = false
    private var shopCategoryDiscountName : String = ""
    private var shopDiscountPercentage : Float = 0f
    private var shopDiscountMinimumPrice : Float = 0f

    private lateinit var firebaseFirestore : FirebaseFirestore
    private lateinit var tabLayout : TabLayout
    private lateinit var viewPagerMainProducts : ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            shop_key = it.getString(arpan.delivery.ui.home.shop_key)
            param2 = it.getString(ARG_PARAM2)
            shop_name = it.getString("shop_name")
            shop_location = it.getString("shop_location")
            cover_image = it.getString("cover_image")
            image = it.getString("image")
            deliver_charge = it.getString("deliver_charge")
            da_charge = it.getString("da_charge")
            shopNotice = it.getString("shopNotice")
            shopNoticeColor = it.getString("shopNoticeColor")
            shopNoticeColorBg = it.getString("shopNoticeColorBg")

            shopDiscount = it.getBoolean("shopDiscount")
            shopCategoryDiscount = it.getBoolean("shopCategoryDiscount")
            shopCategoryDiscountName = it.getString("shopCategoryDiscountName").toString()
            shopDiscountPercentage = it.getFloat("shopDiscountPercentage")
            shopDiscountMinimumPrice = it.getFloat("shopDiscountMinimumPrice")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_products, container, false)
    }

    private fun initDialogProgress(context: Context) {
        (context as HomeActivity).showProgressDialog()
    }

    private fun dismissDialogProgress(context: Context) {
        (context as HomeActivity).hideProgressDialog()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initDialogProgress(view.context)
        initVars(view)
        loadFirestoreShopCategories(view.context)
        placeTexts(view)
    }

    private fun placeTexts(view: View) {
        view.shopNameText.text = shop_name
        view.locationText.text = shop_location

        if(cover_image != ""){
            val storageReference = FirebaseStorage.getInstance()
                    .getReference(Constants.FS_SHOPS_MAIN)
                    .child(shop_key.toString())
                    .child(cover_image.toString())

            Glide.with(view.context)
                    .load(storageReference)
                    .centerCrop()
                    .placeholder(R.drawable.loading_image_glide).into(view.coverImageView)
        }
        if(image != ""){
            val storageReference = FirebaseStorage.getInstance()
                    .getReference(Constants.FS_SHOPS_MAIN)
                    .child(shop_key.toString())
                    .child(image.toString())

            Glide.with(view.context)
                    .load(storageReference)
                    .override(512,512)
                    .placeholder(R.drawable.loading_image_glide)
                .into(view.shopImageItem)
        }

        (view.context as HomeActivity).titleActionBarTextView.text = shop_name
        (view.context as HomeActivity).deleteItemsFromCart.visibility = View.GONE
        (view.context as HomeActivity).img_cart_icon.visibility = View.VISIBLE
    }

    private fun loadFirestoreShopCategories(context: Context) {
        firebaseFirestore.collection(Constants.FC_SHOPS_MAIN_CATEGORY)
                .document(Constants.FD_PRODUCTS_MAIN_CATEGORY)
                .collection(Constants.FD_PRODUCTS_MAIN_CATEGORY)
                .document(shop_key.toString())
                .addSnapshotListener{ value, error ->
                    error?.printStackTrace()
                    dismissDialogProgress(context)
                    if(value!!.data!=null){
                        val categoryItemsArray = ArrayList<ProductCategoryItem>()
                        val map = value!!.data as Map<String, Map<String,String>>
                        for(category_field in map.entries){
                            categoryItemsArray.add(
                                ProductCategoryItem(
                                    key = category_field.key,
                                    name = category_field.value[Constants.FIELD_FD_PRODUCTS_MAIN_CATEGORY_NAME].toString(),
                                    category_key = category_field.value[Constants.FIELD_FD_PRODUCTS_MAIN_CATEGORY_KEY].toString(),
                                    order = category_field.value[Constants.FIELD_FD_PRODUCTS_MAIN_CATEGORY_ORDER].toString().toInt(),
                                )
                            )
                        }
                        Collections.sort(categoryItemsArray, kotlin.Comparator { o1, o2 ->
                            (o1.order).compareTo(o2.order) })
                        viewPagerMainProducts.adapter = ViewPagerAdapterProducts(context as HomeActivity,
                            categoryItemsArray, shop_key.toString(), shopDiscount, shopCategoryDiscount,
                            shopCategoryDiscountName, shopDiscountPercentage, shopDiscountMinimumPrice)
                        TabLayoutMediator(tabLayout, viewPagerMainProducts
                        ) { tab, position ->
                            tab.text = categoryItemsArray[position].name
                        }.attach()
                    }
                }
    }

    private fun initVars(view: View) {
        firebaseFirestore = FirebaseFirestore.getInstance()
        tabLayout = view.tabLayoutProducts
        viewPagerMainProducts = view.viewPagerMainProducts
        if(shopNotice!!.isNotEmpty()){
            view.linearLayout6.visibility = View.VISIBLE
            view.specialOfferTextView.text = shopNotice
            view.specialOfferTextView.setTextColor(Color.parseColor(shopNoticeColor))
            view.specialOfferTextView.setBackgroundColor(Color.parseColor(shopNoticeColorBg))
        }else{
            view.linearLayout6.visibility = View.GONE
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProductsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProductsFragment().apply {
                arguments = Bundle().apply {
                    putString(arpan.delivery.ui.home.shop_key, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}