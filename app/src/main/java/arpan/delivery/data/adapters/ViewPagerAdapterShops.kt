package arpan.delivery.data.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import arpan.delivery.data.models.ShopCategoryItem
import arpan.delivery.ui.fragments.CategorizedShops

class ViewPagerAdapterShops(fragmentManager : FragmentManager,
                            private val dataList: ArrayList<ShopCategoryItem>) :
    FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private var mPageReferenceMap = HashMap<Int, Fragment>()

    fun getFragment(key: Int): Fragment {
        return mPageReferenceMap[key]!!
    }

    override fun getCount(): Int {
        return dataList.size
    }

    override fun getItem(position: Int): Fragment {
        val myFragment: Fragment = CategorizedShops.newInstance(dataList[position].category_key, "")
        mPageReferenceMap[position] = myFragment
        return myFragment
    }
}