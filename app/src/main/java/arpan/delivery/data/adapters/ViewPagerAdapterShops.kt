package arpan.delivery.data.adapters

import android.content.Context
import android.os.Parcelable
import android.view.View
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager

private class ViewPagerAdapterShops(ctx: Context, data: List<String>) :
    PagerAdapter() {
    private val data: List<String> = data
    private val ctx: Context = ctx
    override fun getCount(): Int {
        return data.size
    }

    override fun instantiateItem(collection: View, position: Int): Any {
        val view = TextView(ctx)
        view.text = data[position]
        (collection as ViewPager).addView(view)
        return view
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun saveState(): Parcelable? {
        return null
    }

    override fun restoreState(arg0: Parcelable?, arg1: ClassLoader?) {}

}