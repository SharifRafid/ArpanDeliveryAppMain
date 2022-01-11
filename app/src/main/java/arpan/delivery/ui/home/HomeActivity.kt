package arpan.delivery.ui.home

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
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
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
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
import arpan.delivery.data.models.ShopItem
import arpan.delivery.ui.cart.CartItemsViewModelFactory
import arpan.delivery.ui.cart.CartViewModel
import arpan.delivery.ui.launcher.MainActivity
import arpan.delivery.utils.Constants
import arpan.delivery.utils.createProgressDialog
import arpan.delivery.utils.showToast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.shashank.sony.fancytoastlib.FancyToast
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.call_dialog_view.view.*
import kotlinx.android.synthetic.main.dialog_add_category.view.*
import kotlinx.android.synthetic.main.dialog_alert_layout_main.view.*
import kotlinx.android.synthetic.main.fragment_cart.view.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_order.view.*
import kotlinx.android.synthetic.main.pop_up_to_take_to_cart.view.*
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
    lateinit var dataSnapshotOrderTakingTime: DataSnapshot
    lateinit var popUpForCartRedirect : PopupWindow

    var firstLaunch = true

    var mainShopsArrayList = ArrayList<ShopItem>()

    var popUpWindowOpen = false
    var popupWindow = PopupWindow()

    var userNameFromProfile = ""
    var userAddressFromProfile = ""

    var completeCountOfListeners = ArrayList<Boolean>()

    private var deepLinkChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLanguageDefault()
        setContentView(R.layout.activity_home)
        setTheme(R.style.Theme_ArpanDelivery)
        initVar()
        initLogic()
        initCartLogic()
        initFabMenu()
        initUserDetailsFetch()
        initAppUpdateStatusCheck()
    }

    private fun initAppUpdateStatusCheck() {
        FirebaseDatabase.getInstance().reference.child("APP_UPDATE_STATUS")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val mandatory = snapshot.child("mandatory").getValue(Boolean::class.java) as Boolean
                    val minVersion = snapshot.child("minVersion").getValue(Long::class.java) as Long
                    val info = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                    if(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            info.longVersionCode < minVersion
                        } else {
                            info.versionCode < minVersion
                        }
                    ){ if(mandatory){
                            val view = LayoutInflater.from(this@HomeActivity)
                                .inflate(R.layout.dialog_alert_layout_main, null)
                            val dialog = AlertDialog.Builder(this@HomeActivity)
                                .setView(view).create()
                            dialog.setCancelable(false)
                            dialog.setCanceledOnTouchOutside(false)
                            view.btnNoDialogAlertMain.text = getString(R.string.no)
                            view.btnYesDialogAlertMain.text = "আপডেট করুন"
                            view.titleTextView.text = "অ্যাপ টি দ্রুত আপডেট করুন"
                            view.messageTextView.visibility = View.VISIBLE
                            view.messageTextView.text = "আপনি অ্যাপটি আপডেট করা ছাড়া ব্যবহার করতে পারবেন না। ধন্যবাদ।"
                            view.btnNoDialogAlertMain.setOnClickListener {
                                dialog.dismiss()
                                finish()
                            }
                            view.btnYesDialogAlertMain.setOnClickListener {
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
                                dialog.dismiss()
                                finish()
                            }
                            dialog.show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    error.toException().printStackTrace()
                }

            })
    }

    private fun initUserDetailsFetch() {
        if(FirebaseAuth.getInstance().currentUser!=null){
            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            firebaseFirestore.collection("users")
                .document(uid)
                .addSnapshotListener { value, error ->
                    if(value!=null){
                        userNameFromProfile = value.getString("name").toString()
                        userAddressFromProfile = value.getString("address").toString()
                    }
                }
        }
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
        val popUpForCartRedirectView = LayoutInflater.from(this).inflate(R.layout.pop_up_to_take_to_cart, null)
        popUpForCartRedirect = PopupWindow(popUpForCartRedirectView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT, true)
        //popUpForCartRedirect.setWindowLayoutType(WindowManager.LayoutParams.TYPE_TOAST);
        popUpForCartRedirect.animationStyle = R.anim.popupwindowanimation
        popUpForCartRedirectView.cardViewTakeToCart.setOnClickListener {
            popUpForCartRedirect.dismiss()
            openCartFragment(it)
        }
        popUpForCartRedirect.contentView = popUpForCartRedirectView
        popUpForCartRedirect.isOutsideTouchable = false
        popUpForCartRedirect.isTouchable = true
        popUpForCartRedirect.isFocusable = false
//        (navigationView.menu.findItem(R.id.changeThemeMenuItem).actionView as SwitchMaterial).isClickable = false
//        (navigationView.menu.findItem(R.id.changeThemeMenuItem).actionView as SwitchMaterial).isChecked =
//                AppCompatDelegate.getDefaultNightMode()==AppCompatDelegate.MODE_NIGHT_YES
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
                val shareBody = "Download Arpan App From Google Play https://play.google.com/store/apps/details?id=arpan.delivery"
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
            }
            else if(id == R.id.customOrderNewFragment){
                if(!completeCountOfListeners.contains(false)){
                    if(FirebaseAuth.getInstance().currentUser!=null){
                        navController.navigate(R.id.customOrderNewFragment)
                    }else{
                        showToast("অর্ডার করার জন্য লগ ইন করতে হবে ।", FancyToast.ERROR)
                    }
                }else{
                    showToast("একটু অপেক্ষা করুন", FancyToast.LENGTH_SHORT)
                }
            }else if(id == R.id.medicineNewFragment){
                if(!completeCountOfListeners.contains(false)){
                    if(FirebaseAuth.getInstance().currentUser!=null){
                        navController.navigate(R.id.medicineNewFragment)
                    }else{
                        showToast("অর্ডার করার জন্য লগ ইন করতে হবে ।", FancyToast.ERROR)
                    }
                }else{
                    showToast("একটু অপেক্ষা করুন", FancyToast.LENGTH_SHORT)
                }
            }else if(id == R.id.pickUpDropFragment){
                if(!completeCountOfListeners.contains(false)){
                    if(FirebaseAuth.getInstance().currentUser!=null){
                        navController.navigate(R.id.pickUpDropFragment)
                    }else{
                        showToast("অর্ডার করার জন্য লগ ইন করতে হবে ।", FancyToast.ERROR)
                    }
                }else{
                    showToast("একটু অপেক্ষা করুন", FancyToast.LENGTH_SHORT)
                }
            }else if(id == R.id.parcelNewFragment){
                if(!completeCountOfListeners.contains(false)){
                    if(FirebaseAuth.getInstance().currentUser!=null){
                        navController.navigate(R.id.parcelNewFragment)
                    }else{
                        showToast("অর্ডার করার জন্য লগ ইন করতে হবে ।", FancyToast.ERROR)
                    }
                }else{
                    showToast("একটু অপেক্ষা করুন", FancyToast.LENGTH_SHORT)
                }
            }else if (id == R.id.oldOrderListFragment){
                if(navController.currentDestination!!.id != R.id.oldOrderListFragment){
                    if(!completeCountOfListeners.contains(false)){
                        if(FirebaseAuth.getInstance().currentUser!=null){
                            navController.navigate(R.id.oldOrderListFragment)
                        }else{
                            showToast("পূর্বের অর্ডার দেখার জন্য লগ ইন করতে হবে ।", FancyToast.ERROR)
                        }
                    }else{
                        showToast("একটু অপেক্ষা করুন", FancyToast.LENGTH_SHORT)
                    }
                }
            }else{
                NavigationUI.onNavDestinationSelected(menuItem, navController)
            }
//            else if(id == R.id.changeThemeMenuItem){
//                if(AppCompatDelegate.getDefaultNightMode()!=AppCompatDelegate.MODE_NIGHT_YES){
//                    (menuItem.actionView as SwitchMaterial).isChecked = true
//                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//                }else{
//                    (menuItem.actionView as SwitchMaterial).isChecked = false
//                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//                }
//            }

            drawerMainHome.closeDrawer(GravityCompat.START)
            true
        }
        drawerMainHome.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        getViewPagerImages()
        completeCountOfListeners.add(false)
        completeCountOfListeners.add(false)
        completeCountOfListeners.add(false)
        completeCountOfListeners.add(false)
        completeCountOfListeners.add(false)
        completeCountOfListeners.add(false)
        initiateRealtimeListenerForProductOrderLimits()     //0
        initiateRealtimeListenerForLocationNormalOrders()       //1
        initiateRealtimeListenerForLocationPickDropOrders()     //2
        initiateRealtimeListenerForOrderShopLimits()        //3
        initFirebaseMessaging()     //NONE
        initBottomMenuClicks()      //NONE
        initUserProfileData()       //NONE
        initiateRealtimeListenerForOrderTakingTime()        //4
    }

    fun showPopUpWindowForCart(){
        popUpForCartRedirect.showAtLocation(fabMain, Gravity.BOTTOM, 0, 200)
        Timer().schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    hidePopUpWindowForCart()
                }
            }
        }, 3000)
    }

    fun hidePopUpWindowForCart(){
        if(popUpForCartRedirect.isShowing){
            popUpForCartRedirect.dismiss()
        }
    }

    private fun initiateRealtimeListenerForOrderTakingTime() {
        FirebaseDatabase.getInstance().reference.child("ORDER_TAKING_TIME")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    completeCountOfListeners[4] = true
                    dataSnapshotOrderTakingTime = snapshot
                    checkShouldHideProgressOrNot()
                }

                override fun onCancelled(error: DatabaseError) {
                    error.toException().printStackTrace()
                }

            })
    }

    private fun initUserProfileData() {
        if(FirebaseAuth.getInstance().currentUser!=null){
            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            firebaseFirestore.collection("users")
                    .document(uid)
                .addSnapshotListener { value, error ->
                    error?.printStackTrace()
                    if(value!=null){
                        if(value.getString("name").toString().isEmpty() ||
                            value.getString("address").toString().isEmpty()) {
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
                }
        }

    }

    private fun initBottomMenuClicks() {
        img_old_orders.setOnClickListener {
            if(navController.currentDestination!!.id != R.id.oldOrderListFragment){
                if(!completeCountOfListeners.contains(false)){
                    if(FirebaseAuth.getInstance().currentUser!=null){
                        navController.navigate(R.id.oldOrderListFragment)
                    }else{
                        showToast("পূর্বের অর্ডার দেখার জন্য লগ ইন করতে হবে ।", FancyToast.ERROR)
                    }
                }else{
                    showToast("একটু অপেক্ষা করুন", FancyToast.LENGTH_SHORT)
                }
            }
        }
        img_complain.setOnClickListener { view ->
            val dialog = Dialog(this, R.style.Theme_ArpanDelivery)
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
                }else{
                    showToast(" আপনি কোনো কিছু লিখেন নাই। ", FancyToast.ERROR)
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
                        completeCountOfListeners[3] = true
                        homeViewModel.setMaxShops(
                            snapshot.child("max_shops").getValue(Long::class.java)!!.toInt()
                        )
                        homeViewModel.setDeliveryChargeExtra(
                            snapshot.child("delivery_charge_extra").getValue(
                                Long::class.java
                            )!!.toInt()
                        )
                        homeViewModel.setDAChargeExtra(
                            snapshot.child("da_charge_extra").getValue(
                                Long::class.java
                            )!!.toInt()
                        )
                        homeViewModel.setAllowMoreShops(snapshot.child("allow_more").value as Boolean)
                        checkShouldHideProgressOrNot()
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
                    completeCountOfListeners[2] = true
                    val arrayList = ArrayList<LocationItem>()
                    for (snap in snapshot.children) {
                        arrayList.add(
                            LocationItem(
                                key = snap.key.toString(),
                                locationName = snap.child("name").value.toString(),
                                deliveryCharge = snap.child("deliveryCharge").value.toString().toInt(),
                                daCharge = snap.child("daCharge").value.toString().toInt(),
                            )
                        )
                    }
                    homeViewModel.setLocationArrayPickDrop(arrayList)
                    checkShouldHideProgressOrNot()
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
                    completeCountOfListeners[1] = true
                    val arrayList = ArrayList<LocationItem>()
                    for (snap in snapshot.children) {
                        val locationItem = LocationItem(
                            key = snap.key.toString(),
                            locationName = snap.child("name").value.toString(),
                            deliveryCharge = snap.child("deliveryCharge").value.toString().toInt(),
                            daCharge = snap.child("daCharge").value.toString().toInt(),
                        )
                        if(snap.child("deliveryChargeClient").value != null){
                            locationItem.deliveryChargeClient = snap.child("deliveryChargeClient").value.toString().toInt()
                        }
                        arrayList.add(locationItem)
                    }
                    homeViewModel.setLocationArray(arrayList)
                    checkShouldHideProgressOrNot()
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
                        completeCountOfListeners[0] = true
                        checkShouldHideProgressOrNot()

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
            .addSnapshotListener { value, error ->
                error?.printStackTrace()
                if(value!=null){
                    homeViewModel.setOffersDocumentSnapshotData(MutableLiveData(value))
                    getViewPagerImagesMain()
                }
            }
    }

    private fun getViewPagerImagesMain() {
        firebaseFirestore.collection(Constants.FC_OFFERS_OI)
            .document(Constants.FD_OFFERS_OID)
            .addSnapshotListener { value, error ->
                error?.printStackTrace()
                if(value!=null){
                    homeViewModel.setOffersDocumentSnapshotMainData(MutableLiveData(value))
                }
                firebaseFirestore.collection(Constants.FC_OFFERS_OI)
                    .document("timebased_notifications_document")
                    .addSnapshotListener { value2, error2 ->
                        error2?.printStackTrace()
                        homeViewModel.setTimeBasedNotificationsDocumentSnapshotMainData(
                            MutableLiveData(value2)
                        )
                        firebaseFirestore.collection(Constants.FC_OFFERS_OI)
                            .document("normal_notifications_document")
                            .addSnapshotListener { value3, error3 ->
                                error3?.printStackTrace()
                                if(value3!=null){
                                    homeViewModel.setNormalNotificationsDocumentSnapshotMainData(
                                        MutableLiveData(value3)
                                    )
                                    getCategoriesData()
                                }
                            }
                    }

            }
//        firebaseFirestore.collection(Constants.FC_OFFERS_OI)
//                .document(Constants.FD_OFFERS_OID)
//                .get().addOnCompleteListener {
//                    homeViewModel.setOffersDocumentSnapshotMainData(MutableLiveData(it))
//                    firebaseFirestore.collection(Constants.FC_OFFERS_OI)
//                            .document("timebased_notifications_document")
//                            .get().addOnCompleteListener { task ->
//                                homeViewModel.setTimeBasedNotificationsDocumentSnapshotMainData(
//                                    MutableLiveData(
//                                        task
//                                    )
//                                )
//                                firebaseFirestore.collection(Constants.FC_OFFERS_OI)
//                                        .document("normal_notifications_document")
//                                        .get().addOnCompleteListener { task ->
//                                            homeViewModel.setNormalNotificationsDocumentSnapshotMainData(
//                                                MutableLiveData(
//                                                    task
//                                                )
//                                            )
//                                            getCategoriesData()
//                                        }
//                            }
//            }
//
    }

    private fun getCategoriesData() {
        firebaseFirestore.collection(Constants.FC_SHOPS_MAIN_CATEGORY)
                .document(Constants.FD_SHOPS_MAIN_CATEGORY)
            .addSnapshotListener { value, error ->
                error?.printStackTrace()
                if(value!=null){
                    homeViewModel.setCategoriesDocumentSnapshotData(MutableLiveData(value))
                    loadShopsDataFromFirestore()
                    completeCountOfListeners[5] = true
                    checkShouldHideProgressOrNot()
                }
            }
    }

    private fun loadShopsDataFromFirestore() {
        FirebaseFirestore.getInstance()
            .collection(Constants.FC_SHOPS_MAIN)
            .whereEqualTo(Constants.FIELD_FD_SM_STATUS, "open")
            .orderBy(Constants.FIELD_FD_SM_CATEGORY)
            .orderBy(Constants.FIELD_FD_SM_ORDER)
            .addSnapshotListener { value, error ->
                error?.printStackTrace()
                homeViewModel.setMainShopsDocumentSnapshot(value!!)
                if(firstLaunch){
                    homeViewModel.setStatus(true)
                    navController.navigate(R.id.action_homeFragment_self)
                    firstLaunch = false
                }
            }
    }

    private fun checkShouldHideProgressOrNot() {
        hideProgressDialog()
        if(!completeCountOfListeners.contains(false)){
            checkPopUpStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        checkDynamicLinkStatus()
    }

    fun checkDynamicLinkStatus() {
        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                // Get deep link from result (may be null if no link is found)
                var deepLink: Uri? = null
                if (pendingDynamicLinkData != null) {
                    deepLink = pendingDynamicLinkData.link
                    Log.e("data",deepLink.toString())
                    val shopId = deepLink.toString().removePrefix("http://arpan-app.wixsite.com/arpan-app?=")
                    for(item in mainShopsArrayList){
                        if(item.key == shopId){
                            val bundle = Bundle()
                            bundle.putString("shop_key",item.key)
                            bundle.putString("shop_name",item.name)
                            bundle.putString("shop_location",item.location)
                            bundle.putString("cover_image",item.cover_image)
                            bundle.putString("image",item.image)
                            bundle.putString("deliver_charge",item.deliver_charge)
                            bundle.putString("da_charge",item.da_charge)
                            bundle.putString("shopNotice",item.shopNotice)
                            bundle.putString("shopNoticeColor",item.shopNoticeColor)
                            bundle.putString("shopNoticeColorBg",item.shopNoticeColorBg)
                            if(navController.currentDestination!!.id != R.id.productsFragment){
                                navController.navigate(R.id.productsFragment, bundle)
                            }
                            break
                        }
                    }
                }

                // Handle the deep link. For example, open the linked
                // content, or apply promotional credit to the user's
                // account.
                // ...

                // ...
            }
            .addOnFailureListener(this) { e -> Log.w("TAG_HOME_ACTIVITY", "getDynamicLink:onFailure", e) }
    }

    private fun checkPopUpStatus() {
        checkNotificationPopUpStatus()
        checkOnlinePopUpStatus()
        //checkDynamicLinkStatus()
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
            navController.navigate(R.id.orderHistoryFragment, bundle)
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
        if(!completeCountOfListeners.contains(false)){
            if(navController.currentDestination!!.id != R.id.cartFragment){
                navController.navigate(R.id.cartFragment)
            }
        }else{
            showToast("একটু অপেক্ষা করুন", FancyToast.LENGTH_SHORT)
        }
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