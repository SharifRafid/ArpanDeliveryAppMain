package arpan.delivery.data.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import arpan.delivery.data.models.ProductCategoryItem
import arpan.delivery.data.models.ShopCategoryItem
import arpan.delivery.ui.fragments.CategorizedProducts
import arpan.delivery.ui.fragments.CategorizedShops
import kotlin.collections.ArrayList

class ViewPagerAdapterProducts(fragmentActivity: FragmentActivity,
                               private val dataList : ArrayList<ProductCategoryItem>,
private val shop_key : String) :
    FragmentStateAdapter(fragmentActivity) {
    override fun createFragment(position: Int): Fragment {
        return CategorizedProducts.newInstance(dataList[position].category_key,shop_key)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

}