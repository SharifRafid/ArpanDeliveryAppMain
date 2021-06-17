package arpan.delivery.ui.home

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import arpan.delivery.R
import arpan.delivery.data.db.CartDb
import arpan.delivery.data.db.CartItemsRepo
import arpan.delivery.data.db.CartProductEntity
import arpan.delivery.data.models.LocationItem
import arpan.delivery.ui.cart.CartItemsViewModelFactory
import arpan.delivery.ui.cart.CartViewModel
import arpan.delivery.ui.launcher.MainActivity
import arpan.delivery.utils.Constants
import arpan.delivery.utils.createProgressDialog
import arpan.delivery.utils.showToast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.shashank.sony.fancytoastlib.FancyToast
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.call_dialog_view.view.*
import kotlinx.android.synthetic.main.dialog_add_category.view.*
import kotlinx.android.synthetic.main.dialog_alert_layout_main.view.*
import kotlinx.android.synthetic.main.fragment_order.view.*
import kotlinx.android.synthetic.main.theme_change_button.view.*
import kotlinx.android.synthetic.main.tooltip.view.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class HomeActivity : AppCompatActivity() {

    lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var progress_dialog : Dialog
    lateinit var homeViewModel: HomeViewModel
    lateinit var cartViewModel: CartViewModel
    private var firebaseFirestore = FirebaseFirestore.getInstance()
    var cartItemsAllMainList = ArrayList<CartProductEntity>()
    lateinit var sets : HashSet<String>

    var popUpWindowOpen = false
    var popupWindow = PopupWindow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLanguageDefault()
        setContentView(R.layout.activity_home)
        initVar()
        initLogic()
        initCartLogic()
        initFabMenu()
    }

    private fun initFabMenu() {
        val gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                if(popUpWindowOpen){
                    popupWindow.dismiss()
                    popUpWindowOpen = false
                }else{
                    popUpWindowOpen = true
                    showPopupWindow(fabMain)
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                navController.navigate(R.id.action_homeFragment_self)
                //your action here for double tap e.g.
                //Log.d("OnDoubleTapListener", "onDoubleTap");
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return true
            }

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }
        })
        fabMain.setOnTouchListener(View.OnTouchListener { v, event -> gd.onTouchEvent(event) })
    }

    private fun initCartLogic() {
        cartViewModel.cartItems.observe(this, androidx.lifecycle.Observer {
            cartItemsAllMainList.clear()
            cartItemsAllMainList.addAll(it)
            var size = 0
            sets = HashSet()
            cartItemsAllMainList.forEach { item ->
                size += item.product_item_amount
                if (item.product_item) {
                    sets.add(item.product_item_shop_key)
                }
            }
            Log.e("SET", sets.toString())
            updateTopCartCountText(size)
        })
    }

    private fun showPopupWindow(anchor: View) {
        val toolTipView = LayoutInflater.from(this).inflate(R.layout.tooltip, null, false)
        PopupWindow(anchor.context).apply {
            contentView = toolTipView.apply {
                measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
            }
        }.also { popupWindow ->
            this.popupWindow = popupWindow
            toolTipView.facebookPage.setOnClickListener {
                popupWindow.dismiss()
                val facebookId = "fb://page/101457328287762"
                val urlPage = "http://facebook.com/arpan.delivery"
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(facebookId)))
                } catch (e: java.lang.Exception) {
                    Log.e("ERROR FB PAGE", "Application not intalled.")
                    //Open url web page.
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlPage)))
                }
            }
            toolTipView.email.setOnClickListener {
                popupWindow.dismiss()
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.putExtra(Intent.EXTRA_SUBJECT, "About Arpon Delivery App")
                intent.data = Uri.parse("mailto:arpan.delivery@gmail.com")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            toolTipView.aboutArpon.setOnClickListener {
                popupWindow.dismiss()
                if(navController.currentDestination!!.id != R.id.aboutArpan){
                    navController.navigate(R.id.aboutArpan)
                }
            }
            toolTipView.clientBe.setOnClickListener {
                popupWindow.dismiss()
                if(navController.currentDestination!!.id != R.id.beClient){
                    navController.navigate(R.id.beClient)
                }
            }
            popupWindow.setOnDismissListener {
                popUpWindowOpen = false
            }
            popupWindow.setBackgroundDrawable(BitmapDrawable())
            popupWindow.isOutsideTouchable = true
            val location = IntArray(2).apply {
                anchor.getLocationOnScreen(this)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val size = Size(
                    popupWindow.contentView.measuredWidth,
                    popupWindow.contentView.measuredHeight
                )
                popupWindow.showAtLocation(
                    anchor,
                    Gravity.TOP or Gravity.START,
                    location[0] - (size.width - anchor.width) / 2,
                    location[1] - size.height
                )
            }else{
                popupWindow.showAtLocation(
                    anchor, Gravity.TOP or Gravity.START,
                    0, 0
                )
            }
        }
    }

    private fun updateTopCartCountText(size: Int) {
        if(size==0){
            cartItemText.visibility = View.GONE
        }else{
            cartItemText.visibility = View.VISIBLE
            cartItemText.text = size.toString()
        }
    }

    private fun initLogic() {
        setSupportActionBar(toolBar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(navigationView, navController)
        (navigationView.menu.findItem(R.id.changeThemeMenuItem).actionView as SwitchMaterial).isClickable = false
        (navigationView.menu.findItem(R.id.changeThemeMenuItem).actionView as SwitchMaterial).isChecked =
                AppCompatDelegate.getDefaultNightMode()==AppCompatDelegate.MODE_NIGHT_YES
        navigationView.setNavigationItemSelectedListener{ menuItem ->
            val id = menuItem.itemId
            //it's possible to do more actions on several items, if there is a large amount of items I prefer switch(){case} instead of if()
            if (id == R.id.logoutNowItem) {
                if(FirebaseAuth.getInstance().currentUser!=null){
                    logOutNow(View(this))
                }else{
                    showToast(getString(R.string.you_are_not_logged_in), FancyToast.ERROR)
                }
            }else if(id == R.id.shareTheApp){
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"
                val shareBody = "Download Arpan App From Google Play https://play.google.com/store/apps/details?id=com.dubd.arponapp"
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Share Arpan App")
                sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
                startActivity(Intent.createChooser(sharingIntent, "Share via"))
            }else if(id == R.id.rateTheApp){
                val uri: Uri = Uri.parse("market://details?id=$packageName")
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                // To count with Play market backstack, After pressing back button,
                // to taken back to our application, we need to add following flags to intent.
                goToMarket.addFlags(
                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                                Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                )
                try {
                    startActivity(goToMarket)
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                            Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("http://play.google.com/store/apps/details?id=$packageName")
                            )
                    )
                }
            }else if(id == R.id.changeThemeMenuItem){
                if(AppCompatDelegate.getDefaultNightMode()!=AppCompatDelegate.MODE_NIGHT_YES){
                    (menuItem.actionView as SwitchMaterial).isChecked = true
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }else{
                    (menuItem.actionView as SwitchMaterial).isChecked = false
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
            NavigationUI.onNavDestinationSelected(menuItem, navController)
            drawerMainHome.closeDrawer(GravityCompat.START)
            true
        }
        drawerMainHome.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        getViewPagerImages()
        initiateRealtimeListenerForProductOrderLimits()
        initiateRealtimeListenerForLocationNormalOrders()
        initiateRealtimeListenerForLocationPickDropOrders()
        initiateRealtimeListenerForOrderShopLimits()
        initFirebaseMessaging()
        initBottomMenuClicks()
        initUserProfileData()
    }

    private fun initUserProfileData() {
        if(FirebaseAuth.getInstance().currentUser!=null){
            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            firebaseFirestore.collection("users")
                    .document(uid)
                    .get().addOnCompleteListener {
                        if(it.isSuccessful){
                            if(it.result!=null){
                                if(it.result!!.getString("name").toString().isEmpty()){
                                    val view = LayoutInflater.from(this@HomeActivity)
                                            .inflate(R.layout.dialog_alert_layout_main, null)
                                    val dialog = AlertDialog.Builder(this@HomeActivity)
                                            .setView(view).create()
                                    view.btnNoDialogAlertMain.text = getString(R.string.no)
                                    view.btnYesDialogAlertMain.text = getString(R.string.ok_text)
                                    view.titleTextView.text = getString(R.string.complete_profile)
                                    view.messageTextView.text = getString(R.string.please_complete_profile)
                                    view.btnNoDialogAlertMain.setOnClickListener {
                                        dialog.dismiss()
                                    }
                                    view.btnYesDialogAlertMain.setOnClickListener {
                                        dialog.dismiss()
                                        navController.navigate(R.id.profileFragment)
                                    }
                                    dialog.show()
                                }
                            }
                        }else {
                            it.exception!!.printStackTrace()
                        }
                    }
        }

    }

    private fun initBottomMenuClicks() {
        img_old_orders.setOnClickListener {
            if(navController.currentDestination!!.id != R.id.oldOrderListFragment){
                navController.navigate(R.id.oldOrderListFragment)
            }
        }
        img_complain.setOnClickListener { view ->
            val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window!!.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            val dialogView = LayoutInflater.from(this)
                    .inflate(R.layout.dialog_add_category, null)
            dialogView.title_text_view.setOnClickListener {
                dialog.dismiss()
            }
            dialogView.addProductCategoriesButton.setOnClickListener {
                if(dialogView.edt_shop_name.text.toString().isNotEmpty()){
                    dialogView.addProductCategoriesButton.isEnabled = false
                    dialogView.addProductCategoriesButton.text = "সাবমিট করা হচ্ছে"
                    val tokenArray: MutableMap<String, Any> = HashMap()
                    tokenArray["feedbacks"] = FieldValue
                            .arrayUnion(dialogView.edt_shop_name.text.toString())
                    FirebaseFirestore.getInstance().collection("feedbacks")
                            .document("feedbacks")
                            .update(tokenArray).addOnCompleteListener {
                                if(it.isSuccessful){
                                    dialog.dismiss()
                                    FancyToast.makeText(
                                        this, "সাবমিট করা হয়েছে",
                                        FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false
                                    ).show()
                                }else{
                                    dialog.dismiss()
                                    FancyToast.makeText(
                                        this, "সাবমিট করা হয়নি",
                                        FancyToast.LENGTH_SHORT, FancyToast.ERROR,
                                        false
                                    ).show()

                                }
                            }
                }
            }
            dialog.setContentView(dialogView)
            dialog.show()
            KeepStatusBar()
        }
    }

    fun callNowButtonClicked(view: View){
        if (homeViewModel.callPermissionCheck(this, this)) {
            val dialog = Dialog(this)

            val view2 = LayoutInflater.from(this).inflate(R.layout.call_dialog_view, null)

            view2.call1.setOnClickListener {
                val callIntent = Intent(
                        Intent.ACTION_CALL,
                        Uri.parse("tel:" + "+8801621716166")
                )
                startActivity(callIntent)
                dialog.dismiss()
            }
            view2.call2.setOnClickListener {
                val callIntent2 = Intent(
                        Intent.ACTION_CALL,
                        Uri.parse("tel:" + "+8801845568015")
                )
                startActivity(callIntent2)
                dialog.dismiss()
            }
            view2.call3.setOnClickListener {
                val callIntent3 = Intent(
                        Intent.ACTION_CALL,
                        Uri.parse("tel:" + "+8801701007680")
                )
                startActivity(callIntent3)
                dialog.dismiss()
            }

            dialog.setContentView(view2)

            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }
    }

    fun messageNowButtonClicked(view: View){
        val messengerUrl: String = if (isMessengerAppInstalled()) {
            "fb-messenger://m.me/arpan.delivery"
        } else {
            "https://m.me/arpan.delivery"
        }
        val messengerIntent = Intent(Intent.ACTION_VIEW)
        messengerIntent.data = Uri.parse(messengerUrl)
        startActivity(messengerIntent)
    }

    //step 2, required
    private fun KeepStatusBar() {
        val attrs = window.attributes
        attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
        window.attributes = attrs
    }

    private fun isMessengerAppInstalled(): Boolean {
        return try {
            applicationContext.packageManager.getApplicationInfo("com.facebook.orca", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun initiateRealtimeListenerForOrderShopLimits() {
        FirebaseDatabase.getInstance()
                .reference
                .child("data")
                .child("order_shop_limits")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        homeViewModel.setMaxShops(
                            snapshot.child("max_shops").getValue(Long::class.java)!!.toInt()
                        )
                        homeViewModel.setDeliveryChargeExtra(
                            snapshot.child("delivery_charge_extra").getValue(
                                Long::class.java
                            )!!.toInt()
                        )
                        homeViewModel.setAllowMoreShops(snapshot.child("allow_more").value as Boolean)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        error.toException().printStackTrace()
                    }

                })
    }

    private fun initFirebaseMessaging() {
        if(FirebaseAuth.getInstance().currentUser!=null){
            FirebaseMessaging.getInstance().isAutoInitEnabled = true
            val token = getSharedPreferences("FCM_TOKEN", MODE_PRIVATE)
                    .getString("TOKEN", "")
            FirebaseMessaging.getInstance().token.addOnCompleteListener(
                OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        return@OnCompleteListener
                    }
                    val t = task.result!!
                    Log.e("TOKEN", t)
                    if (token != t) {
                        val tokenArray: MutableMap<String, Any> = HashMap()
                        tokenArray["registrationTokens"] = FieldValue.arrayUnion(t)
                        val map = HashMap<String, String>()
                        map["registration_token"] = t
                        getSharedPreferences("FCM_TOKEN", MODE_PRIVATE)
                            .edit().putString("TOKEN", t).apply()
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(FirebaseAuth.getInstance().currentUser!!.uid)
                            .update(tokenArray)
                    }
                })
        }
    }

    private fun initiateRealtimeListenerForLocationPickDropOrders() {
        FirebaseDatabase.getInstance()
            .reference
                .child("data")
                .child("delivery_charges_pick_drop")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val arrayList = ArrayList<LocationItem>()
                    for (snap in snapshot.children) {
                        arrayList.add(
                            LocationItem(
                                locationName = snap.key.toString(),
                                deliveryCharge = snap.value.toString().toInt()
                            )
                        )
                    }
                    homeViewModel.setLocationArrayPickDrop(arrayList)
                }

                override fun onCancelled(error: DatabaseError) {
                    error.toException().printStackTrace()
                }

            })
    }

    private fun initiateRealtimeListenerForLocationNormalOrders() {
        FirebaseDatabase.getInstance()
            .reference
                .child("data")
                .child("delivery_charges")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val arrayList = ArrayList<LocationItem>()
                    for (snap in snapshot.children) {
                        arrayList.add(
                            LocationItem(
                                locationName = snap.key.toString(),
                                deliveryCharge = snap.value.toString().toInt()
                            )
                        )
                    }
                    homeViewModel.setLocationArray(arrayList)
                }

                override fun onCancelled(error: DatabaseError) {
                    error.toException().printStackTrace()
                }

            })
    }

    private fun initiateRealtimeListenerForProductOrderLimits() {
        FirebaseDatabase.getInstance().reference
                .child("data")
                .child("order_custom_limits")
                .addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                        error.toException().printStackTrace()
                    }

                    override fun onDataChange(snapshot: DataSnapshot) {
                        homeViewModel.setCategoriesMaxOrderLimitParcel(
                            snapshot.child("parcel").value.toString().toInt()
                        )
                        homeViewModel.setCategoriesMaxOrderLimitCustomOrder(
                            snapshot.child("custom_cat").value.toString().toInt()
                        )
                        homeViewModel.setCategoriesMaxOrderLimitMedicine(
                            snapshot.child("medicine").value.toString().toInt()
                        )
                        homeViewModel.setCategoriesMaxOrderLimit(
                            snapshot.child("max_categories").value.toString().toInt()
                        )
                    }

                })
    }

    private fun getViewPagerImages() {
        showProgressDialog()
        firebaseFirestore.collection(Constants.FC_OFFERS_OI)
                .document(Constants.FD_OFFERS_OIS)
                .get().addOnCompleteListener {
                    homeViewModel.setOffersDocumentSnapshotData(MutableLiveData(it))
                    getViewPagerImagesMain()
                }
    }

    private fun getViewPagerImagesMain() {
        firebaseFirestore.collection(Constants.FC_OFFERS_OI)
                .document(Constants.FD_OFFERS_OID)
                .get().addOnCompleteListener {
                    homeViewModel.setOffersDocumentSnapshotMainData(MutableLiveData(it))
                    firebaseFirestore.collection(Constants.FC_OFFERS_OI)
                            .document("timebased_notifications_document")
                            .get().addOnCompleteListener { task ->
                                homeViewModel.setTimeBasedNotificationsDocumentSnapshotMainData(
                                    MutableLiveData(
                                        task
                                    )
                                )
                                firebaseFirestore.collection(Constants.FC_OFFERS_OI)
                                        .document("normal_notifications_document")
                                        .get().addOnCompleteListener { task ->
                                            homeViewModel.setNormalNotificationsDocumentSnapshotMainData(
                                                MutableLiveData(
                                                    task
                                                )
                                            )
                                            getCategoriesData()
                                        }
                            }
                }
    }

    private fun getCategoriesData() {
        firebaseFirestore.collection(Constants.FC_SHOPS_MAIN_CATEGORY)
                .document(Constants.FD_SHOPS_MAIN_CATEGORY)
                .get().addOnCompleteListener {
                    homeViewModel.setCategoriesDocumentSnapshotData(MutableLiveData(it))
                    homeViewModel.setStatus(true)
                    navController.navigate(R.id.action_homeFragment_self)
                    hideProgressDialog()
                    checkPopUpStatus()
                }
    }

    private fun checkPopUpStatus() {
        checkNotificationPopUpStatus()
        checkOnlinePopUpStatus()
    }

    private fun checkOnlinePopUpStatus() {
        FirebaseDatabase.getInstance().reference
                .child("emergency_dialog_data")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.child("state").value.toString() == "active") {
                            val view = LayoutInflater.from(this@HomeActivity)
                                .inflate(R.layout.dialog_alert_layout_main, null)
                            val dialog = AlertDialog.Builder(this@HomeActivity)
                                .setView(view).create()
                            view.btnNoDialogAlertMain.visibility = View.GONE
                            view.btnYesDialogAlertMain.text = getString(R.string.ok_text)
                            view.titleTextView.text = snapshot.child("title").value.toString()
                            view.messageTextView.text = snapshot.child("body").value.toString()
                            view.btnYesDialogAlertMain.setOnClickListener {
                                dialog.dismiss()
                            }
                            dialog.show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        error.toException().printStackTrace()
                    }

                })
    }

    private fun checkNotificationPopUpStatus() {
        if(intent.hasExtra("popup")){
            Log.e("FDSFSF", intent.getStringExtra("apidialogtitle").toString())
            Log.e("FDSFSF", intent.getStringExtra("apidialogtitle2").toString())
            val view = LayoutInflater.from(this)
                    .inflate(R.layout.dialog_alert_layout_main, null)
            val dialog = AlertDialog.Builder(this)
                    .setView(view).create()
            view.btnNoDialogAlertMain.visibility = View.GONE
            view.btnYesDialogAlertMain.text = getString(R.string.ok_text)
            view.titleTextView.text = intent.getStringExtra("apidialogtitle").toString()
            view.messageTextView.text = intent.getStringExtra("apidialogtitle2").toString()
            view.btnYesDialogAlertMain.setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
        }
        if(intent.hasExtra("orderID")){
            val bundle = Bundle()
            bundle.putString("orderID", intent.getStringExtra("orderID").toString())
            navController.navigate(R.id.action_homeFragment_to_orderHistoryFragment, bundle)
        }
    }

    private fun initVar() {
        navController = Navigation.findNavController(this, R.id.main_home_fragment_container)
        appBarConfiguration = AppBarConfiguration.Builder(navController.graph).setOpenableLayout(
            drawerMainHome
        ).build()
        progress_dialog = createProgressDialog()
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        cartViewModel = ViewModelProvider(
            this,
            CartItemsViewModelFactory(
                CartItemsRepo(
                    CartDb.getInstance(applicationContext).cartDao
                )
            )
        ).get(CartViewModel::class.java)
    }

    private fun setLanguageDefault() {
        val language = getSharedPreferences("APP_SETTINGS", MODE_PRIVATE)
            .getString("lang", "bn")
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        if(Build.VERSION.SDK_INT >= 17) { config.setLocale(locale); } else { config.locale = locale; }
        baseContext.resources.updateConfiguration(
            config,
            baseContext.resources.displayMetrics
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
    }

    fun showProgressDialog(){
        if(!progress_dialog.isShowing){
            progress_dialog.show()
        }
    }

    fun hideProgressDialog(){
        if(progress_dialog.isShowing){
            progress_dialog.dismiss()
        }
    }

    fun openCartFragment(view: View) {
        navController.navigate(R.id.cartFragment)
    }

    fun deleteAllItemsFromCart(view2: View) {
        if(cartItemsAllMainList.isNotEmpty()){
            val view = LayoutInflater.from(this@HomeActivity)
                    .inflate(R.layout.dialog_alert_layout_main, null)
            val dialog = AlertDialog.Builder(this@HomeActivity)
                    .setView(view).create()
            view.btnNoDialogAlertMain.text = getString(R.string.no)
            view.btnYesDialogAlertMain.text = getString(R.string.ok_text)
            view.titleTextView.text = getString(R.string.delete)
            view.messageTextView.text = getString(R.string.are_you_sure_to_delete_all_the_products)
            view.btnNoDialogAlertMain.setOnClickListener {
                dialog.dismiss()
            }
            view.btnYesDialogAlertMain.setOnClickListener {
                dialog.dismiss()
                cartViewModel.deleteAll()
                showToast(getString(R.string.cart_cleared), FancyToast.SUCCESS)
                navController.navigate(R.id.action_cartFragment_to_homeFragment_clearTop)
            }
            dialog.show()
        }
    }

    fun testClickData(view: View) {
        // DATA ADDING FUNCTION FOR TESTING
    }

    fun logOutNow(view2: View) {
        val view = LayoutInflater.from(this@HomeActivity)
                .inflate(R.layout.dialog_alert_layout_main, null)
        val dialog = AlertDialog.Builder(this@HomeActivity)
                .setView(view).create()
        view.btnNoDialogAlertMain.text = getString(R.string.no)
        view.btnYesDialogAlertMain.text = getString(R.string.yes)
        view.titleTextView.text = getString(R.string.are_you_sure_to_log_out)
        view.messageTextView.visibility = View.GONE
        view.btnNoDialogAlertMain.setOnClickListener {
            dialog.dismiss()
        }
        view.btnYesDialogAlertMain.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
            dialog.dismiss()
        }
        dialog.show()
    }
}