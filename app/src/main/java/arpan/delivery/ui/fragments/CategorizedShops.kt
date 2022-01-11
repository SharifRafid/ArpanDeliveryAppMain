package arpan.delivery.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arpan.delivery.R
import arpan.delivery.data.adapters.ShopItemRecyclerAdapter
import arpan.delivery.data.models.ShopItem
import arpan.delivery.utils.Constants
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_categorized_products.view.*
import kotlinx.android.synthetic.main.fragment_categorized_shops.view.*
import kotlinx.android.synthetic.main.fragment_categorized_shops.view.mainRecyclerView
import kotlinx.android.synthetic.main.fragment_categorized_shops.view.shopsProgress
import kotlin.collections.ArrayList


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class CategorizedShops : Fragment() {
    private var filter_cat_word: String? = null
    private var param2: String? = null
    private lateinit var firebaseQuery : Query
    private val arrayList = ArrayList<ShopItem>()
    private lateinit var lastDocument : DocumentSnapshot
    private lateinit var recyclerAdapterMainShops : ShopItemRecyclerAdapter
    private val TAG = "CategorizedShops"
    private lateinit var mainView : View
    private var hasMoreData = false

    var isScrolling = false
    var currentItems = 0
    var totalItems:Int = 0
    var scrollOutItems:Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            filter_cat_word = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_categorized_shops, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        view.mainRecyclerView.layoutManager = linearLayoutManager
        recyclerAdapterMainShops = ShopItemRecyclerAdapter(view.context, arrayList, "")
        view.mainRecyclerView.adapter = recyclerAdapterMainShops
        mainView = view

        firebaseQuery.get()
                .addOnCompleteListener {
                    arrayList.clear()
                    if(it.isSuccessful){
                        if(it.result!!.documents.isNotEmpty()){
                            for(document in it.result!!.documents){
                                arrayList.add(
                                        ShopItem(
                                                key = document.id,
                                                name = document.getString(Constants.FIELD_FD_SM_NAME).toString(),
                                                categories = document.getString(Constants.FIELD_FD_SM_CATEGORY).toString(),
                                                image = document.getString(Constants.FIELD_FD_SM_ICON).toString(),
                                                cover_image = document.getString(Constants.FIELD_FD_SM_COVER).toString(),
                                                da_charge = document.getString(Constants.FIELD_FD_SM_DA_CHARGE).toString(),
                                                deliver_charge = document.getString(Constants.FIELD_FD_SM_DELIVERY).toString(),
                                                location = document.getString(Constants.FIELD_FD_SM_LOCATION).toString(),
                                                username = document.getString(Constants.FIELD_FD_SM_USERNAME).toString(),
                                                password = document.getString(Constants.FIELD_FD_SM_PASSWORD).toString(),
                                                order = document.getString(Constants.FIELD_FD_SM_ORDER).toString().toInt()
                                        )
                                )
                            }
                            if(arrayList.size%Constants.MAIN_SHOPS_PAGING_ARRAYLIST_SIZE==0) {
                                hasMoreData = true
                            }
                            lastDocument = it.result!!.documents[it.result!!.documents.size-1]
                            recyclerAdapterMainShops.notifyDataSetChanged()
                            view.shopsProgress.visibility = View.GONE
                            view.mainRecyclerView.visibility = View.VISIBLE
                        }
                    }else{
                        it.exception!!.printStackTrace()
                    }
                }
    }

    fun getData() {
        if(hasMoreData){
            if(arrayList.size%Constants.MAIN_SHOPS_PAGING_ARRAYLIST_SIZE==0) {
                mainView.shopsProgress.visibility = View.VISIBLE
                firebaseQuery.startAfter(lastDocument).get()
                        .addOnCompleteListener {
                            if(it.isSuccessful){
                                if(it.result!!.documents.isNotEmpty()){
                                    for(document in it.result!!.documents){
                                        arrayList.add(
                                                ShopItem(
                                                        key = document.id,
                                                        name = document.getString(Constants.FIELD_FD_SM_NAME).toString(),
                                                        categories = document.getString(Constants.FIELD_FD_SM_CATEGORY).toString(),
                                                        image = document.getString(Constants.FIELD_FD_SM_ICON).toString(),
                                                        cover_image = document.getString(Constants.FIELD_FD_SM_COVER).toString(),
                                                        da_charge = document.getString(Constants.FIELD_FD_SM_DA_CHARGE).toString(),
                                                        deliver_charge = document.getString(Constants.FIELD_FD_SM_DELIVERY).toString(),
                                                        location = document.getString(Constants.FIELD_FD_SM_LOCATION).toString(),
                                                        username = document.getString(Constants.FIELD_FD_SM_USERNAME).toString(),
                                                        password = document.getString(Constants.FIELD_FD_SM_PASSWORD).toString(),
                                                        order = document.getString(Constants.FIELD_FD_SM_ORDER).toString().toInt()
                                                )
                                        )
                                    }
                                    lastDocument = it.result!!.documents[it.result!!.documents.size-1]
                                    recyclerAdapterMainShops.notifyDataSetChanged()
                                    mainView.shopsProgress.visibility = View.GONE
                                }else{
                                    hasMoreData = false
                                    mainView.shopsProgress.visibility = View.GONE
                                }
                            }else{
                                it.exception!!.printStackTrace()
                            }
                        }
            }
        }

    }

    fun getItemCountOfArrayList(): Int {
        return arrayList.size
    }

    companion object {
        fun newInstance(filter_cat_word: String, param2: String) =
                CategorizedShops().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, filter_cat_word)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}

//view.mainRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//        super.onScrollStateChanged(recyclerView, newState)
//        if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
//            isScrolling = true
//        }
//    }
//
//    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//        super.onScrolled(recyclerView, dx, dy)
//        currentItems = linearLayoutManager.childCount
//        totalItems = linearLayoutManager.itemCount
//        Log.e(TAG, currentItems.toString())
//        Log.e(TAG, totalItems.toString())
//        scrollOutItems = linearLayoutManager.findFirstVisibleItemPosition()
//        if (isScrolling && currentItems + scrollOutItems == totalItems) {
//            isScrolling = false
//            if(arrayList.size%10==0){
//                getData()
//            }
//        }
//    }
//})