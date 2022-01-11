package arpan.delivery.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arpan.delivery.R
import arpan.delivery.data.adapters.ProductItemRecyclerAdapter
import arpan.delivery.data.models.ProductItem
import arpan.delivery.ui.home.HomeActivity
import arpan.delivery.utils.Constants
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_categorized_products.view.*
import kotlinx.android.synthetic.main.fragment_categorized_shops.view.mainRecyclerView
import kotlinx.android.synthetic.main.fragment_categorized_shops.view.shopsProgress


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class CategorizedProducts : Fragment() {
    private var filter_cat_word: String? = null
    private var shop_key: String? = null
    private lateinit var firebaseQuery : Query
    private lateinit var lastDocument : DocumentSnapshot
    private var hasMoreData = false
    private val productsMainArrayList = ArrayList<ProductItem>()
    private lateinit var adapter : ProductItemRecyclerAdapter
    private var loadingNewData = false

    private var shopDiscount : Boolean = false
    private var shopCategoryDiscount : Boolean = false
    private var shopCategoryDiscountName : String = ""
    private var shopDiscountPercentage : Float = 0f
    private var shopDiscountMinimumPrice : Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            filter_cat_word = it.getString(ARG_PARAM1)
            shop_key = it.getString(ARG_PARAM2)
            shopDiscount = it.getBoolean("shopDiscount")
            shopCategoryDiscount = it.getBoolean("shopCategoryDiscount")
            shopCategoryDiscountName = it.getString("shopCategoryDiscountName").toString()
            shopDiscountPercentage = it.getFloat("shopDiscountPercentage")
            shopDiscountMinimumPrice = it.getFloat("shopDiscountMinimumPrice")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_categorized_products, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        view.mainRecyclerView.layoutManager = linearLayoutManager

        adapter = ProductItemRecyclerAdapter(this, view.context,
            view.context as Activity,
            productsMainArrayList,shop_key.toString(),filter_cat_word.toString(),shop_key.toString()
            , (activity as HomeActivity).cartViewModel,
            shopDiscount, shopCategoryDiscount, shopCategoryDiscountName,
            shopDiscountPercentage, shopDiscountMinimumPrice)
        view.mainRecyclerView.adapter = adapter

        firebaseQuery = FirebaseFirestore.getInstance().collection(Constants.FC_SHOPS_MAIN)
            .document(shop_key.toString())
            .collection(Constants.FD_PRODUCTS_MAIN_SUB_COLLECTION)
            .whereEqualTo("shopCategoryKey", filter_cat_word)
            .whereEqualTo("inStock", "active")
            .orderBy("order")
            .limit(Constants.MAIN_SHOPS_PAGING_ARRAYLIST_SIZE.toLong())

        //runFirstQuery(view)
        runFirstQueryWithSnapshotListenerForFasterPerformance(view)
    }

    private fun runFirstQueryWithSnapshotListenerForFasterPerformance(view: View) {
        FirebaseFirestore.getInstance().collection(Constants.FC_SHOPS_MAIN)
            .document(shop_key.toString())
            .collection(Constants.FD_PRODUCTS_MAIN_SUB_COLLECTION)
            .whereEqualTo("shopCategoryKey", filter_cat_word)
            .whereEqualTo("inStock", "active")
            .orderBy("order")
            .addSnapshotListener { value, error ->
                error?.printStackTrace()
                if(value!=null){
                    if(value.documents.isNotEmpty()){
                        productsMainArrayList.clear()
                        for(document in value.documents){
                            val p = document.toObject(ProductItem::class.java)!!
                            p.key = document.id
                            productsMainArrayList.add(p)
                        }
                        adapter.notifyDataSetChanged()
                        view.shopsProgress.visibility = View.GONE
                        view.mainRecyclerView.visibility = View.VISIBLE
                        view.noProductsText.visibility = View.GONE
                    }else{
                        //context?.showToast(getString(R.string.no_data_fount), FancyToast.WARNING)
                        view.shopsProgress.visibility = View.GONE
                        view.mainRecyclerView.visibility = View.GONE
                        view.noProductsText.visibility = View.VISIBLE
                    }
                }
            }
    }

    private fun runFirstQuery(view : View) {
        firebaseQuery.get().addOnCompleteListener {
                if(it.isSuccessful){
                    if(it.result!!.documents.isNotEmpty()){
                        productsMainArrayList.clear()
                        for(document in it.result!!.documents){
                            val p = document.toObject(ProductItem::class.java)!!
                            p.key = document.id
                            productsMainArrayList.add(p)
                        }
                        adapter.notifyDataSetChanged()
                        hasMoreData = productsMainArrayList.size%Constants.MAIN_SHOPS_PAGING_ARRAYLIST_SIZE==0
                        lastDocument = it.result!!.documents[it.result!!.documents.size - 1]
                        view.shopsProgress.visibility = View.GONE
                        view.mainRecyclerView.visibility = View.VISIBLE
                        view.noProductsText.visibility = View.GONE
                        loadingNewData = false
                        implementScrollListener(view)
                    }else{
                        //context?.showToast(getString(R.string.no_data_fount), FancyToast.WARNING)
                        view.shopsProgress.visibility = View.GONE
                        view.mainRecyclerView.visibility = View.GONE
                        view.noProductsText.visibility = View.VISIBLE
                    }
                }else{
                    it.exception!!.printStackTrace()
                }
            }
    }

    private fun implementScrollListener(view: View) {
        view.mainRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    hasMoreData = productsMainArrayList.size % Constants.MAIN_SHOPS_PAGING_ARRAYLIST_SIZE == 0
                    if (hasMoreData) {
                        if (!loadingNewData) {
                            loadingNewData = true
                            getData(view)
                        }
                    }
                }
            }
        })
    }

    private fun getData(view: View) {
        if(hasMoreData){
            if(productsMainArrayList.size%Constants.MAIN_SHOPS_PAGING_ARRAYLIST_SIZE==0) {
                firebaseQuery.startAfter(lastDocument).get()
                    .addOnCompleteListener {
                        if(it.isSuccessful){
                            if(it.result!!.documents.isNotEmpty()){
                                for(document in it.result!!.documents){
                                    productsMainArrayList.add(document.toObject(ProductItem::class.java)!!)
                                }
                                lastDocument = it.result!!.documents[it.result!!.documents.size - 1]
                                adapter.notifyDataSetChanged()
                                loadingNewData = false
                            }else{
                                hasMoreData = false
                            }
                        }else{
                            it.exception!!.printStackTrace()
                        }
                    }
            }
        }

    }

    companion object {
        fun newInstance(
            filter_cat_word: String,
            param2: String,
            shopDiscount: Boolean,
            shopCategoryDiscount: Boolean,
            shopCategoryDiscountName: String,
            shopDiscountPercentage: Float,
            shopDiscountMinimumPrice: Float
        ) =
                CategorizedProducts().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, filter_cat_word)
                        putString(ARG_PARAM2, param2)
                        putBoolean("shopDiscount", shopDiscount)
                        putBoolean("shopCategoryDiscount", shopCategoryDiscount)
                        putString("shopCategoryDiscountName", shopCategoryDiscountName)
                        putFloat("shopDiscountPercentage", shopDiscountPercentage)
                        putFloat("shopDiscountMinimumPrice", shopDiscountMinimumPrice)
                    }
                }
    }
}