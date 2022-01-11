package arpan.delivery.data.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import arpan.delivery.data.models.ProductCategoryItem
import arpan.delivery.ui.fragments.CategorizedProducts
import kotlin.collections.ArrayList

class ViewPagerAdapterProducts(
    fragmentActivity: FragmentActivity,
    private val dataList: ArrayList<ProductCategoryItem>,
    private val shop_key: String,
    private val shopDiscount: Boolean,
    private val shopCategoryDiscount: Boolean,
    private val shopCategoryDiscountName: String,
    private val shopDiscountPercentage: Float,
    private val shopDiscountMinimumPrice: Float
) :
    FragmentStateAdapter(fragmentActivity) {
    override fun createFragment(position: Int): Fragment {
        return CategorizedProducts.newInstance(dataList[position].category_key,shop_key,
            shopDiscount, shopCategoryDiscount, shopCategoryDiscountName,
            shopDiscountPercentage, shopDiscountMinimumPrice)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

}