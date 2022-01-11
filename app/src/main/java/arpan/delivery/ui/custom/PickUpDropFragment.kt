package arpan.delivery.ui.custom

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import arpan.delivery.R
import arpan.delivery.data.adapters.OrderProductItemRecyclerAdapter
import arpan.delivery.data.models.OrderImageUploadItem
import arpan.delivery.data.models.OrderItemMain
import arpan.delivery.data.models.PickDropOrderItem
import arpan.delivery.ui.home.HomeActivity
import arpan.delivery.ui.home.HomeViewModel
import arpan.delivery.ui.launcher.MainActivity
import arpan.delivery.utils.showToast
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.shashank.sony.fancytoastlib.FancyToast
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.dialog_progress_layout_main.view.*
import kotlinx.android.synthetic.main.fragment_medicine_new.view.*
import kotlinx.android.synthetic.main.fragment_order.view.*
import kotlinx.android.synthetic.main.fragment_pick_up_drop.*
import kotlinx.android.synthetic.main.fragment_pick_up_drop.view.*
import kotlinx.android.synthetic.main.fragment_pick_up_drop.view.bkash_charge_note
import kotlinx.android.synthetic.main.fragment_pick_up_drop.view.imageView
import kotlinx.android.synthetic.main.fragment_pick_up_drop.view.radioGroup
import kotlinx.android.synthetic.main.fragment_pick_up_drop.view.spinner_1
import kotlinx.android.synthetic.main.fragment_pick_up_drop.view.spinner_2
import kotlinx.android.synthetic.main.fragment_pick_up_drop.view.txt_place_order
import kotlinx.android.synthetic.main.fragment_pick_up_drop.view.txt_price
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PickUpDropFragment : Fragment() {

    private lateinit var v : View
    private lateinit var contextMain: Context
    private var allPrice = 0
    private var daCharge = 0
    private var deliveryLocations = ArrayList<String>()
    private var deliveryCharges = ArrayList<Int>()
    private var imagePath : Uri = Uri.parse("")
    private var imageName = "PickDropImageName"
    private lateinit var dialogViewCustomAnimation : View
    private lateinit var dialog : Dialog

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        v = inflater.inflate(R.layout.fragment_pick_up_drop, container, false)
        contextMain = container!!.context

        var snapshot = (v.context as HomeActivity).dataSnapshotOrderTakingTime
        val startTime = snapshot.child("start_time").value.toString()
        val endTime = snapshot.child("end_time").value.toString()
        val over_time_orders = snapshot.child("over_time_orders").value.toString()
        if(over_time_orders == "yes"){
            v.txt_place_order.visibility = View.VISIBLE
        }else{
            val string1 = "${startTime}:00"
            val time1 = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).parse(string1)
            val calendar1 = Calendar.getInstance()
            calendar1.time = time1
            calendar1.add(Calendar.DATE, 1)
            val string2 = "${endTime}:00"
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
            if(x.after(calendar1.time) && x.before(calendar2.time)) {
                v.txt_place_order.visibility = View.VISIBLE
            }else{
                v.context.showToast(getString(R.string.shgop_order_time_ehon_bodro_ache), FancyToast.ERROR)
                v.txt_place_order.visibility = View.GONE
            }
        }

        (v.context as HomeActivity).titleActionBarTextView.text = getString(R.string.pick_and_drop_page_title)
        (v.context as HomeActivity).deleteItemsFromCart.visibility = View.GONE
        (v.context as HomeActivity).img_cart_icon.visibility = View.VISIBLE

        val sharedPreferences = contextMain.getSharedPreferences("pick_drop_data",MODE_PRIVATE)

        val homeViewModel = activity?.let { ViewModelProvider(it).get(HomeViewModel::class.java) }!!
        for(item in homeViewModel.getLocationArrayPickDrop()) {
            deliveryCharges.add(item.deliveryCharge)
            deliveryLocations.add(item.locationName)
        }
        val adapter = ArrayAdapter(
                contextMain,
                R.layout.custom_spinner_view,
                deliveryLocations
        )

        adapter.setDropDownViewResource(R.layout.custom_spinner_item_view)

        v.edt_name.setText((v.context as HomeActivity).userNameFromProfile)
        v.edt_mobile.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber!!.toString())
        v.edt_address.setText((v.context as HomeActivity).userAddressFromProfile)
        v.edt_aboutParcel.setText(sharedPreferences.getString("edt_aboutParcel",""))
        v.edt_name_reciver.setText(sharedPreferences.getString("edt_name_reciver",""))
        v.edt_mobile_reciver.setText(sharedPreferences.getString("edt_mobile_reciver",""))
        v.edt_address_reciver.setText(sharedPreferences.getString("edt_address_reciver",""))

        imagePath = Uri.parse(sharedPreferences.getString("photo",""))
        if(imagePath.toString().isNotEmpty()){
            v.imageView.setImageURI(imagePath)
        }

        v.imageView.setOnClickListener {
//          val choose = Intent(Intent.ACTION_PICK,
//                  MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//          startActivityForResult(choose, PICK_IMAGE_CODE)

            ImagePicker.with(this)
                    .crop()
                    .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
                    .start()
        }

        v.edt_aboutParcel.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                sharedPreferences.edit().putString("edt_aboutParcel",s.toString())
                        .apply()
            }
            override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
            ) {}
            override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
            ) {
            }
        })
        v.edt_name_reciver.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                sharedPreferences.edit().putString("edt_name_reciver",s.toString())
                        .apply()
            }
            override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
            ) {}
            override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
            ) {
            }
        })
        v.edt_mobile_reciver.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                sharedPreferences.edit().putString("edt_mobile_reciver",s.toString())
                        .apply()
            }
            override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
            ) {}
            override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
            ) {
            }
        })
        v.edt_address_reciver.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                sharedPreferences.edit().putString("edt_address_reciver",s.toString())
                        .apply()
            }
            override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
            ) {}
            override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
            ) {
            }
        })

        v.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            if(checkedId == R.id.rb1){
                v.bkash_charge_note.visibility = View.VISIBLE
            }else{
                v.bkash_charge_note.visibility = View.GONE
            }
        }

        v.spinner_1.adapter = adapter

        v.spinner_2.adapter = adapter

        v.spinner_1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
            ) {
                val pos1 = position
                val pos2 = v.spinner_2.selectedItemPosition

                if(pos1 == 0 || pos2 == 0){
                    allPrice = calculatePrice(pos1, pos2)

                    v.txt_price.text = "মোটঃ $allPrice টাকা"
                }else{
                    allPrice = 0
                    daCharge = 0
                    v.txt_price.text = "মূল্য ফোন কলের মাধ্যমে নিশ্চিত করা হবে"
                }

                if(pos1==0){
                    v.text_address_field_1.visibility = View.VISIBLE
                }else{
                    v.text_address_field_1.visibility = View.GONE
                }
                if(pos2==0){
                    v.text_addess_field_2.visibility = View.VISIBLE
                }else{
                    v.text_addess_field_2.visibility = View.GONE
                }

            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                v.spinner_1.setSelection(0)
            }
        }

        v.spinner_2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
            ) {
                val pos1 = position
                val pos2 = v.spinner_1.selectedItemPosition

                if(pos1 == 0 || pos2 == 0){
                    allPrice = calculatePrice(pos1, pos2)
                    v.txt_price.text = "মোটঃ "+" "+allPrice+" "+"টাকা"
                }else{
                    allPrice = 0
                    daCharge = 0
                    v.txt_price.text = getString(R.string.mullo_phone_call_er_maddhome)
                }

                if(pos2==0){
                    v.text_address_field_1.visibility = View.VISIBLE
                }else{
                    v.text_address_field_1.visibility = View.GONE
                }
                if(pos1==0){
                    v.text_addess_field_2.visibility = View.VISIBLE
                }else{
                    v.text_addess_field_2.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                v.spinner_2.setSelection(0)
            }
        }

        fun isNetworkConnected(): Boolean {
            val cm = contextMain.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            return cm!!.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
        }

        v.txt_place_order.setOnClickListener {
            if(isNetworkConnected()){
                if (v.edt_name.text.isNotEmpty() &&
                        v.edt_mobile.text.isNotEmpty() &&
                        v.edt_aboutParcel.text.isNotEmpty() &&
                        v.edt_name_reciver.text.isNotEmpty()&&
                        v.edt_mobile_reciver.text.isNotEmpty()
                ) {
                    val pickDropItem = PickDropOrderItem()
                    pickDropItem.senderName = v.edt_name.text.toString()
                    pickDropItem.senderPhone = v.edt_mobile.text.toString()
                    pickDropItem.senderAddress = v.edt_address.text.toString()
                    pickDropItem.senderLocation = deliveryLocations[spinner_1.selectedItemPosition]
                    pickDropItem.parcelDetails = v.edt_aboutParcel.text.toString()
                    pickDropItem.recieverName = v.edt_name_reciver.text.toString()
                    pickDropItem.recieverPhone = v.edt_mobile_reciver.text.toString()
                    pickDropItem.recieverAddress = v.edt_address_reciver.text.toString()
                    pickDropItem.recieverLocation = deliveryLocations[spinner_2.selectedItemPosition]
                    pickDropItem.paymentType = ""
                    placeOrderFinally(v, pickDropItem)
                } else {
                    FancyToast.makeText(
                            context, getString(R.string.filll_all_fields),
                            FancyToast.LENGTH_SHORT, FancyToast.ERROR, false
                    ).show()
                }
            }else{
                FancyToast.makeText(
                        contextMain, getString(R.string.ensure_interne),
                        FancyToast.LENGTH_SHORT, FancyToast.ERROR, false
                ).show()
            }
        }
        return v
    }


    private fun placeOrderFinally(view: View, pickDropOrderItem : PickDropOrderItem) {
        dialogViewCustomAnimation = LayoutInflater.from(view.context).inflate(R.layout.dialog_progress_layout_main, null)
        dialog = AlertDialog.Builder(view.context)
                .setView(dialogViewCustomAnimation)
                .create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        val key = "ORDER"+System.currentTimeMillis()
        val orderItemMain = OrderItemMain()
        orderItemMain.key = key
        orderItemMain.userPhoneAccount = FirebaseAuth.getInstance().currentUser!!.phoneNumber!!
        orderItemMain.userId = FirebaseAuth.getInstance().currentUser!!.uid
        orderItemMain.userPhoneAccount = FirebaseAuth.getInstance().currentUser!!.phoneNumber!!
        orderItemMain.paymentMethod = if(view.radioGroup.checkedRadioButtonId == R.id.rb1){
            "bKash"
        }else{
            "COD"
        }
        orderItemMain.totalPrice = 0
        orderItemMain.deliveryCharge = allPrice
        orderItemMain.daCharge = daCharge
        orderItemMain.orderPlacingTimeStamp = System.currentTimeMillis()
        orderItemMain.lastTouchedTimeStamp = System.currentTimeMillis()
        val firebaseStorage = FirebaseStorage.getInstance().reference.child("ORDER_IMAGES")
                .child(key)
        orderItemMain.pickDropOrder = true
        orderItemMain.pickDropOrderItem = pickDropOrderItem
        if(imagePath.toString().isEmpty()){
            placeOrderFinalUpload(view, orderItemMain)
        }else{
            firebaseStorage.child(key)
                    .putFile(imagePath)
                    .addOnCompleteListener {
                        if(it.isSuccessful){
                            orderItemMain.pickDropOrderItem.parcelImage = key
                            placeOrderFinalUpload(view, orderItemMain)
                        }else{
                            view.context.showToast(getString(R.string.failed_order), FancyToast.ERROR)
                            dialog.dismiss()
                        }
                    }
        }

    }


    private fun placeOrderFinalUpload(view: View, orderItemMain: OrderItemMain) {
        FirebaseDatabase.getInstance().reference.child("orderNumberNew")
                .child("ON")
                .get().addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        if(task.result!!.value==null){
                            orderItemMain.orderId = "ARP1001"
                            FirebaseDatabase.getInstance().reference.child("orderNumberNew")
                                    .child("ON").setValue("1001")
                        }else{
                            orderItemMain.orderId = "ARP"+(task.result!!.value.toString().toInt()+1)
                            FirebaseDatabase.getInstance().reference.child("orderNumberNew")
                                    .child("ON").setValue((task.result!!.value.toString().toInt()+1).toString())
                        }
                        dialogViewCustomAnimation.animationView.setAnimation(R.raw.uploading_completed)
                        FirebaseFirestore.getInstance().collection("users")
                                .document(FirebaseAuth.getInstance().currentUser!!.uid)
                                .collection("users_order_collection")
                                .add(orderItemMain)
                                .addOnCompleteListener {
                                    if(it.isSuccessful){
                                        view.context.showToast(getString(R.string.order_placed_successfully), FancyToast.SUCCESS)
                                        (view.context as HomeActivity).onBackPressed()
                                        contextMain.getSharedPreferences("pick_drop_data",MODE_PRIVATE)
                                                .edit().clear().apply()
                                        val bundle = Bundle()
                                        bundle.putString("orderID",it.result.id)
                                        (view.context as HomeActivity).navController.navigate(R.id.action_homeFragment_self, bundle)
                                        (view.context as HomeActivity).navController.navigate(R.id.orderHistoryFragment, bundle)
                                        dialog.dismiss()
                                    }else{
                                        dialog.dismiss()
                                        it.exception!!.printStackTrace()
                                        view.context.showToast(getString(R.string.failed_order), FancyToast.ERROR)
                                    }
                                }
                    }else{
                        dialog.dismiss()
                        task.exception!!.printStackTrace()
                        view.context.showToast(getString(R.string.failed_order), FancyToast.ERROR)
                    }
                }
    }

    fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)
        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar: Calendar = Calendar.getInstance()
        calendar.setTimeInMillis(milliSeconds)
        return formatter.format(calendar.getTime())
    }

    private fun calculatePrice(pos1: Int, pos2: Int): Int {
        val selectedItemId = if(pos1==0 && pos2==0) {
            0
        }else if(pos1!=0) {
            pos1
        }else{
            pos2
        }
        daCharge = (activity as HomeActivity).homeViewModel.getLocationArrayPickDrop()[selectedItemId]
            .daCharge
        return deliveryCharges[selectedItemId]
    }

    private fun saveKey(key: String) {
        val sharedPreferences = contextMain.getSharedPreferences("cartItems", Context.MODE_PRIVATE)
        val str = sharedPreferences.getString("item", "")
        if(str==""){
            sharedPreferences.edit().putString("item", key).apply()
        }else{
            sharedPreferences.edit().putString("item", "$str,$key").apply()
        }
    }

    override fun onActivityResult(
            requestCode: Int,
            resultCode: Int,
            data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
//          if (requestCode == PICK_IMAGE_CODE && resultCode == AppCompatActivity.RESULT_OK) {
//              val fullPhotoUri = data!!.data
//              v.imageView.setImageURI(fullPhotoUri)
//              imagePath = fullPhotoUri!!
//              imageName = getFileName(imagePath)!!
//          }

        if(resultCode == Activity.RESULT_OK) {
            val fullPhotoUri = data!!.data
            v.imageView.setImageURI(fullPhotoUri)
            contextMain
                    .getSharedPreferences("medicine_data", Context.MODE_PRIVATE)
                    .edit().putString("photo",fullPhotoUri.toString()).apply()
            imagePath = fullPhotoUri!!
            imageName = getFileName(imagePath)!!
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = contextMain.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor!!.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }
}