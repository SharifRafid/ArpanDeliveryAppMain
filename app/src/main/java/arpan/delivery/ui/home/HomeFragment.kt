package arpan.delivery.ui.home

import android.content.Context
import android.net.ParseException
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arpan.delivery.R
import arpan.delivery.data.adapters.ShopItemRecyclerAdapter
import arpan.delivery.data.adapters.SliderAdapterExample
import arpan.delivery.data.adapters.SliderAdapterExampleTB
import arpan.delivery.data.adapters.TopMenuRecyclerAdapter
import arpan.delivery.data.models.OfferImage
import arpan.delivery.data.models.ShopCategoryItem
import arpan.delivery.data.models.ShopItem
import arpan.delivery.data.models.SlidingTextItem
import arpan.delivery.utils.Constants
import arpan.delivery.utils.showToast
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shashank.sony.fancytoastlib.FancyToast
import com.smarteist.autoimageslider.IndicatorView.animation.type.IndicatorAnimationType
import com.smarteist.autoimageslider.SliderAnimations
import com.smarteist.autoimageslider.SliderView
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var firebaseFirestore : FirebaseFirestore
    private lateinit var tabLayout : TabLayout
    private lateinit var mainView : View

    private var arrayListSize = 0
    private val TAG = "HomeFragment"
    private lateinit var firebaseQuery : Query
    private val arrayList = ArrayList<ShopItem>()
    private lateinit var lastDocument : DocumentSnapshot
    private lateinit var recyclerAdapterMainShops : ShopItemRecyclerAdapter
    private var hasMoreData = false
    private var filter_cat_word = ""
    private lateinit var linearLayoutManager : LinearLayoutManager
    private var loadingNewData = false
    private lateinit var homeViewModel: HomeViewModel

    val mainArrayListWithData = ArrayList<ShopItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ):
            View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initVars(view)
        loadRestLogic(view)
    }

    private fun loadRestLogic(view: View) {
        initTopMenuItems(view.context, view.topRecyclerView)
        homeViewModel.dataLoaded().observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if(it){
                initViewPagerOfferImagesStatusCheck(view.context, view.imageSlider)
                initMainShopsLoadingAndPlacingProcess(view.context)
                initViewPagerTimebasedNotificationItems(view.context, view.textSliderTimeBased)
                initViewPagerNormalNotificationItems(view.context, view.textSliderNormal)
            }
        })
    }

    private fun initVars(view: View) {
        firebaseFirestore = FirebaseFirestore.getInstance()
        tabLayout = view.tabLayout
        mainView = view
        linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        mainRecyclerViewShopsHome.layoutManager = linearLayoutManager
        recyclerAdapterMainShops = ShopItemRecyclerAdapter(view.context, arrayList, "")
        mainRecyclerViewShopsHome.adapter = recyclerAdapterMainShops
        firebaseQuery = FirebaseFirestore.getInstance()
                .collection(Constants.FC_SHOPS_MAIN)
                .whereEqualTo(Constants.FIELD_FD_SM_STATUS, "open")
                .whereEqualTo(Constants.FIELD_FD_SM_CATEGORY, filter_cat_word)
                .orderBy(Constants.FIELD_FD_SM_ORDER)
                .limit(Constants.MAIN_SHOPS_PAGING_ARRAYLIST_SIZE.toLong())
//        view.swipeRefreshLayoutHome.setOnRefreshListener {
//            view.swipeRefreshLayoutHome.isRefreshing = false
//            loadRestLogic(view)
//        }
        homeViewModel = activity?.let { ViewModelProvider(it).get(HomeViewModel::class.java) }!!

        (view.context as HomeActivity).titleActionBarTextView.text = getString(R.string.arpan)
        (view.context as HomeActivity).deleteItemsFromCart.visibility = View.GONE
        (view.context as HomeActivity).img_cart_icon.visibility = View.VISIBLE
    }

    private fun initViewPagerOfferImagesStatusCheck(context: Context, imageSlider: SliderView) {
        homeViewModel.getOffersDocumentSnapshotData().observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if(it.contains(Constants.FIELD_FD_OFFERS_OIS)){
                if(it.getBoolean(Constants.FIELD_FD_OFFERS_OIS) == true){
                    mainView.imageSlider.visibility = View.VISIBLE
                    initViewPagerOfferImages(context, imageSlider)
                }else{
                    mainView.imageSlider.visibility = View.GONE
                }
            }else{
                mainView.imageSlider.visibility = View.GONE
            }
        })
    }

    private fun initViewPagerTimebasedNotificationItems(context: Context, viewpager: SliderView) {
        homeViewModel.getTimeBasedNotificationsDocumentSnapshotMainData().observe(viewLifecycleOwner,
                androidx.lifecycle.Observer {
                    if (it.data!!.entries.isNotEmpty()) {
                        val imagesList = ArrayList<SlidingTextItem>()
                        val map = it.data!! as HashMap<String, HashMap<String, Any>>
                        for (docField in map.entries) {
                            val d = SlidingTextItem()
                            d.key = docField.key
                            d.enabled = docField.value["enabled"] as Boolean
                            d.textTitle = docField.value["textTitle"] as String
                            d.textDescription = docField.value["textDescription"] as String
                            d.timeBased = docField.value["timeBased"] as Boolean
                            d.startTime = docField.value["startTime"] as Long
                            d.endTime = docField.value["endTime"] as Long
                            d.backgroundColorHex = docField.value["backgroundColorHex"] as String
                            d.textColorHex = docField.value["textColorHex"] as String
                            d.order = docField.value["order"] as Long
                            d.startTimeString = docField.value["startTimeString"] as String
                            d.endTimeString = docField.value["endTimeString"] as String
                            try {
                                val string1 = "${d.startTimeString}:00"
                                val time1 = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).parse(string1)
                                val calendar1 = Calendar.getInstance()
                                calendar1.time = time1
                                calendar1.add(Calendar.DATE, 1)
                                val string2 = "${d.endTimeString}:00"
                                val time2 = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).parse(string2)
                                val calendar2 = Calendar.getInstance()
                                calendar2.time = time2
                                calendar2.add(Calendar.DATE, 1)
                                val someRandomTime = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(Calendar.getInstance().time)
                                val date = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).parse(someRandomTime)
                                val calendar3 = Calendar.getInstance()
                                calendar3.time = date
                                calendar3.add(Calendar.DATE, 1)
                                Log.e("C3", date.toString())
                                Log.e("C2", time2.toString())
                                Log.e("C1", time1.toString())
                                val x = calendar3.time
                                if (d.timeBased && d.enabled) {
                                    if(x.after(calendar1.time) && x.before(calendar2.time)) {
                                        imagesList.add(d)
                                    }
                                }
                            } catch (e: ParseException) {
                                e.printStackTrace()
                                continue
                            }
                        }
                        if (imagesList.isEmpty()) {
                            viewpager.visibility = View.GONE
                        } else {
                            Collections.sort(imagesList, kotlin.Comparator { o1, o2 ->
                                (o1.order).compareTo(o2.order)
                            })
                            val sliderAdapter = SliderAdapterExampleTB(context)
                            sliderAdapter.renewItems(imagesList)
                            viewpager.setSliderAdapter(sliderAdapter)
                            if(imagesList.size>1){
                                viewpager.startAutoCycle()
                            }
                        }
                    } else {
                        viewpager.visibility = View.GONE
                    }
                })
    }

    private fun initViewPagerNormalNotificationItems(context: Context, viewpager: SliderView) {
        homeViewModel.getNormalNotificationsDocumentSnapshotMainData().observe(viewLifecycleOwner,
                androidx.lifecycle.Observer {
                    if(it.data!!.entries.isNotEmpty()){
                        val imagesList = ArrayList<SlidingTextItem>()
                        val map = it.data!! as HashMap<String, HashMap<String,Any>>
                        for(docField in map.entries) {
                            val d = SlidingTextItem()
                            d.key = docField.key
                            d.enabled = docField.value["enabled"] as Boolean
                            d.textTitle = docField.value["textTitle"] as String
                            d.textDescription = docField.value["textDescription"] as String
                            d.timeBased = docField.value["timeBased"] as Boolean
                            d.startTime = docField.value["startTime"] as Long
                            d.endTime = docField.value["endTime"] as Long
                            d.backgroundColorHex = docField.value["backgroundColorHex"] as String
                            d.textColorHex = docField.value["textColorHex"] as String
                            d.order = docField.value["order"] as Long
                            if(d.enabled && !d.timeBased){
                                imagesList.add(d)
                            }
                        }
                        if(imagesList.isEmpty()){
                            viewpager.visibility = View.GONE
                        }else{
                            Collections.sort(imagesList, kotlin.Comparator { o1, o2 ->
                                (o1.order).compareTo(o2.order)
                            })
                            val sliderAdapter = SliderAdapterExampleTB(context)
                            sliderAdapter.renewItems(imagesList)
                            viewpager.setSliderAdapter(sliderAdapter)
                            if(imagesList.size>1){
                                viewpager.startAutoCycle()
                            }
                        }
                    }else{
                        viewpager.visibility = View.GONE
                    }
                })
    }

    private fun initViewPagerOfferImages(context: Context, viewpagerGalleryImages: SliderView) {
        homeViewModel.getOffersDocumentSnapshotMainData().observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if(it.data!!.entries.isNotEmpty()){
                val imagesList = ArrayList<OfferImage>()
                val map = it.data!! as Map<String, Map<String, String>>
                for(docField in map.entries){
                    imagesList.add(
                        OfferImage(
                            key = docField.key,
                            imageLocation = docField.value[Constants.FIELD_FD_OFFERS_OID_LOCATION].toString(),
                            imageDescription = docField.value[Constants.FIELD_FD_OFFERS_OID_DESCRIPTION].toString(),
                            order = docField.value[Constants.FIELD_FD_OFFERS_OID_ORDER].toString().toInt()
                        ))
                }
                Collections.sort(imagesList, kotlin.Comparator { o1, o2 ->
                    (o1.order).compareTo(o2.order)
                })
                val sliderAdapter = SliderAdapterExample(context)
                sliderAdapter.renewItems(imagesList)
                viewpagerGalleryImages.setSliderAdapter(sliderAdapter)
                viewpagerGalleryImages.setSliderTransformAnimation(SliderAnimations.SIMPLETRANSFORMATION)
                viewpagerGalleryImages.autoCycleDirection = SliderView.AUTO_CYCLE_DIRECTION_LEFT
                viewpagerGalleryImages.startAutoCycle()
            }
        })
    }

    private fun initMainShopsLoadingAndPlacingProcess(context: Context) {
        homeViewModel.getCategoriesDocumentSnapshotData().observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            task ->
            val categoryItemsArray = ArrayList<ShopCategoryItem>()
            val map = task.data as Map<String, Map<String, String>>
            for(category_field in map.entries){
                categoryItemsArray.add(
                    ShopCategoryItem(
                        key = category_field.key,
                        name = category_field.value[Constants.FIELD_FD_SHOPS_MAIN_CATEGORY_NAME].toString(),
                        category_key = category_field.value[Constants.FIELD_FD_SHOPS_MAIN_CATEGORY_KEY].toString(),
                        order = category_field.value[Constants.FIELD_FD_SHOPS_MAIN_CATEGORY_ORDER].toString().toInt()
                    )
                )
            }
            Collections.sort(categoryItemsArray, kotlin.Comparator { o1, o2 -> o1.order.compareTo(o2.order) })
            tabLayout.addTab(tabLayout.newTab().setText("সব"))
            for(item in categoryItemsArray){
                tabLayout.addTab(tabLayout.newTab().setText(item.name))
            }
            runTheWholeQueryAtOnceWithoutPagination(categoryItemsArray)
//                tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//                    override fun onTabSelected(tab: TabLayout.Tab?) {
//                        if(tab!!.text=="All"){
//                            firebaseQuery = FirebaseFirestore.getInstance()
//                                    .collection(Constants.FC_SHOPS_MAIN)
//                                    .whereEqualTo(Constants.FIELD_FD_SM_STATUS, "open")
//                                    .orderBy(Constants.FIELD_FD_SM_CATEGORY)
//                                    .orderBy(Constants.FIELD_FD_SM_ORDER)
//                                    .limit(Constants.MAIN_SHOPS_PAGING_ARRAYLIST_SIZE.toLong())
//                            runFirstQuery()
//                        }else{
//                            filter_cat_word = categoryItemsArray[tabLayout.selectedTabPosition-1].category_key
//                            firebaseQuery = FirebaseFirestore.getInstance()
//                                    .collection(Constants.FC_SHOPS_MAIN)
//                                    .whereEqualTo(Constants.FIELD_FD_SM_STATUS, "open")
//                                    .whereEqualTo(Constants.FIELD_FD_SM_CATEGORY, filter_cat_word)
//                                    .orderBy(Constants.FIELD_FD_SM_ORDER)
//                                    .limit(Constants.MAIN_SHOPS_PAGING_ARRAYLIST_SIZE.toLong())
//                            runFirstQuery()
//                        }
//                    }
//
//                    override fun onTabUnselected(tab: TabLayout.Tab?) {
//
//                    }
//
//                    override fun onTabReselected(tab: TabLayout.Tab?) {
//                        if(tab!!.text=="All"){
//                            firebaseQuery = FirebaseFirestore.getInstance()
//                                    .collection(Constants.FC_SHOPS_MAIN)
//                                    .whereEqualTo(Constants.FIELD_FD_SM_STATUS, "open")
//                                    .orderBy(Constants.FIELD_FD_SM_CATEGORY)
//                                    .orderBy(Constants.FIELD_FD_SM_ORDER)
//                                    .limit(Constants.MAIN_SHOPS_PAGING_ARRAYLIST_SIZE.toLong())
//                            runFirstQuery()
//                        }else{
//                            filter_cat_word = categoryItemsArray[tabLayout.selectedTabPosition-1].category_key
//                            firebaseQuery = FirebaseFirestore.getInstance()
//                                    .collection(Constants.FC_SHOPS_MAIN)
//                                    .whereEqualTo(Constants.FIELD_FD_SM_STATUS, "open")
//                                    .whereEqualTo(Constants.FIELD_FD_SM_CATEGORY, filter_cat_word)
//                                    .orderBy(Constants.FIELD_FD_SM_ORDER)
//                                    .limit(Constants.MAIN_SHOPS_PAGING_ARRAYLIST_SIZE.toLong())
//                            runFirstQuery()
//                        }
//                    }
//
//                })
//                tabLayout.selectTab(tabLayout.getTabAt(0))
//                    Collections.sort(categoryItemsArray, kotlin.Comparator { o1, o2 ->
//                        (o1.order).compareTo(o2.order)
//                    })
        })
    }

    private fun initTopMenuItems(context: Context, topRecyclerView: RecyclerView) {
        val titlesTop = arrayListOf("কাস্টম অর্ডার", "ঔষধ", "পার্সেল", "পিক এন্ড ড্রপ")
        val imagesTop = arrayListOf(
                R.drawable.ic_custom_order_icon,
                R.drawable.ic_medicine_icon,
                R.drawable.ic_parcel_icon,
                R.drawable.ic_pickup_and_drop_icon
        )

        topRecyclerView.adapter = TopMenuRecyclerAdapter(context, imagesTop, titlesTop)
        val lm = GridLayoutManager(context, 4)
        lm.orientation = GridLayoutManager.VERTICAL
        topRecyclerView.layoutManager = lm
        topRecyclerView.isNestedScrollingEnabled = false

        cardView1.setOnClickListener {
            if(FirebaseAuth.getInstance().currentUser!=null){
                (context as HomeActivity).navController.navigate(R.id.customOrderNewFragment)
            }else{
                context.showToast("অর্ডার করার জন্য লগ ইন করতে হবে ।", FancyToast.ERROR)
            }
        }
        cardView2.setOnClickListener {
            if(FirebaseAuth.getInstance().currentUser!=null){
                (context as HomeActivity).navController.navigate(R.id.medicineNewFragment)
            }else{
                context.showToast("অর্ডার করার জন্য লগ ইন করতে হবে ।", FancyToast.ERROR)
            }
        }
        cardView3.setOnClickListener {
            if(FirebaseAuth.getInstance().currentUser!=null){
                (context as HomeActivity).navController.navigate(R.id.parcelNewFragment)
            }else{
                context.showToast("অর্ডার করার জন্য লগ ইন করতে হবে ।", FancyToast.ERROR)
            }
        }
        cardView4.setOnClickListener {
            if(FirebaseAuth.getInstance().currentUser!=null){
                (context as HomeActivity).navController.navigate(R.id.pickUpDropFragment)
            }else{
                context.showToast("অর্ডার করার জন্য লগ ইন করতে হবে ।", FancyToast.ERROR)
            }
        }
    }

    private fun runTheWholeQueryAtOnceWithoutPagination(categoryItemsArray: ArrayList<ShopCategoryItem>) {
        // Trying the whole query with snapshot listener to check how it performs or if it
        // does better than paging or not.....
        mainView.mainRecyclerViewShopsHome.visibility = View.GONE
        mainView.mainRecyclerViewShopsHomeProgress.visibility = View.VISIBLE
        val value = homeViewModel.getMainShopsDocumentSnapshot()
        if(value!=null){
            arrayList.clear()
            mainArrayListWithData.clear()
            (activity as HomeActivity).mainShopsArrayList.clear()
            for(document in value.documents){
                val shopItem = ShopItem(
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
                    order = document.getString(Constants.FIELD_FD_SM_ORDER).toString().toInt(),
                )
                if(document.contains("shopNotice")){
                    shopItem.shopNotice = document.getString("shopNotice").toString()
                }
                if(document.contains("shopNoticeColor")){
                    shopItem.shopNoticeColor = document.getString("shopNoticeColor").toString()
                }
                if(document.contains("shopNoticeColorBg")){
                    shopItem.shopNoticeColorBg = document.getString("shopNoticeColorBg").toString()
                }
                if(document.contains("shopDiscount")){
                    shopItem.shopDiscount = document.getBoolean("shopDiscount")!!
                }
                if(document.contains("shopCategoryDiscount")){
                    shopItem.shopCategoryDiscount = document.getBoolean("shopCategoryDiscount")!!
                }
                if(document.contains("shopCategoryDiscountName")){
                    shopItem.shopCategoryDiscountName = document.getString("shopCategoryDiscountName")!!
                }
                if(document.contains("shopDiscountPercentage")){
                    shopItem.shopDiscountPercentage = document.getString("shopDiscountPercentage").toString().toFloat()
                }
                if(document.contains("shopDiscountMinimumPrice")){
                    shopItem.shopDiscountMinimumPrice = document.getString("shopDiscountMinimumPrice").toString().toFloat()
                }

                arrayList.add(shopItem)
                mainArrayListWithData.add(shopItem)
                (activity as HomeActivity).mainShopsArrayList.add(shopItem)
            }
            Collections.sort(arrayList, kotlin.Comparator { o1, o2 ->
                o1.order.compareTo(o2.order)
            })
            Collections.sort(mainArrayListWithData, kotlin.Comparator { o1, o2 ->
                o1.order.compareTo(o2.order)
            })
            Collections.sort((activity as HomeActivity).mainShopsArrayList, kotlin.Comparator { o1, o2 ->
                o1.order.compareTo(o2.order)
            })
            (activity as HomeActivity).checkDynamicLinkStatus()
            recyclerAdapterMainShops.notifyDataSetChanged()
            mainRecyclerViewShopsHome.visibility = View.VISIBLE
            mainRecyclerViewShopsHomeProgress.visibility = View.GONE
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    if(tab!!.text=="সব"){
                        arrayList.clear()
                        arrayList.addAll(mainArrayListWithData)
                        recyclerAdapterMainShops.notifyDataSetChanged()
                    }else{
                        arrayList.clear()
                        filter_cat_word = categoryItemsArray[tabLayout.selectedTabPosition-1].category_key
                        mainArrayListWithData.forEach {shopItem ->
                            if(shopItem.categories.contains(filter_cat_word)){
                                arrayList.add(shopItem)
                            }
                        }
                        recyclerAdapterMainShops.notifyDataSetChanged()
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {

                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                    if(tab!!.text=="সব"){
                        arrayList.clear()
                        arrayList.addAll(mainArrayListWithData)
                        recyclerAdapterMainShops.notifyDataSetChanged()
                    }else{
                        arrayList.clear()
                        filter_cat_word = categoryItemsArray[tabLayout.selectedTabPosition-1].category_key
                        mainArrayListWithData.forEach {shopItem ->
                            if(shopItem.categories.contains(filter_cat_word)){
                                arrayList.add(shopItem)
                            }
                        }
                        recyclerAdapterMainShops.notifyDataSetChanged()
                    }
                }

            })
            tabLayout.selectTab(tabLayout.getTabAt(0))
        }
    }

    private fun runFirstQuery() {
        mainView.mainRecyclerViewShopsHome.visibility = View.GONE
        mainView.mainRecyclerViewShopsHomeProgress.visibility = View.VISIBLE
        arrayList.clear()
        firebaseQuery.get()
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
                            hasMoreData = arrayList.size%Constants.MAIN_SHOPS_PAGING_ARRAYLIST_SIZE==0
                            lastDocument = it.result!!.documents[it.result!!.documents.size - 1]
                            Collections.sort(arrayList, kotlin.Comparator { o1, o2 ->
                                o1.order.compareTo(o2.order)
                            })
                            recyclerAdapterMainShops.notifyDataSetChanged()
                            mainRecyclerViewShopsHome.visibility = View.VISIBLE
                            mainRecyclerViewShopsHomeProgress.visibility = View.GONE
                            loadingNewData = false
                            implementScrollListener()
                        }else{
                            context?.showToast(getString(R.string.no_data_found), FancyToast.WARNING)
                        }
                    }else{
                        it.exception!!.printStackTrace()
                    }
                }
    }

    private fun implementScrollListener() {
        mainRecyclerViewShopsHome.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    hasMoreData = arrayList.size % Constants.MAIN_SHOPS_PAGING_ARRAYLIST_SIZE == 0
                    if (hasMoreData) {
                        if (!loadingNewData) {
                            loadingNewData = true
                            Log.e("PAGINATION DATA CALLED", "SCROLLED TO BOTTOM")
                            getData()
                        }
                    }
                }
            }
        })
    }

    fun getData() {
        if(hasMoreData){
            if(arrayList.size%Constants.MAIN_SHOPS_PAGING_ARRAYLIST_SIZE==0) {
                mainView.mainRecyclerViewShopsHomeProgress.visibility = View.VISIBLE
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
                                    lastDocument = it.result!!.documents[it.result!!.documents.size - 1]
                                    Collections.sort(arrayList, kotlin.Comparator { o1, o2 ->
                                        o1.order.compareTo(o2.order)
                                    })
                                    recyclerAdapterMainShops.notifyDataSetChanged()
                                    mainView.mainRecyclerViewShopsHomeProgress.visibility = View.GONE
                                    arrayListSize = arrayList.size
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
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}