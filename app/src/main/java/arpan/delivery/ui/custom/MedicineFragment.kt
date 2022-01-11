package arpan.delivery.ui.custom

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import arpan.delivery.R
import arpan.delivery.data.db.CartProductEntity
import arpan.delivery.ui.cart.CartViewModel
import arpan.delivery.ui.home.HomeActivity
import arpan.delivery.ui.home.HomeViewModel
import arpan.delivery.utils.createProgressDialog
import com.github.dhaval2404.imagepicker.ImagePicker
import com.shashank.sony.fancytoastlib.FancyToast
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.fragment_medicine.view.*
import kotlinx.android.synthetic.main.fragment_medicine.view.imageView
import kotlinx.android.synthetic.main.fragment_medicine.view.txt_add_to_cart
import kotlinx.android.synthetic.main.fragment_medicine.view.txt_details


class MedicineFragment : Fragment() {
    private lateinit var v : View
    val PICK_IMAGE_CODE = 121
    private var imagePath : Uri = Uri.parse("")
    private lateinit var dialog : Dialog
    private var imageName = "CategoryImageName"
    private lateinit var contextMain: Context
    private lateinit var homeViewModel : HomeViewModel
    private lateinit var cartViewModel : CartViewModel
    private lateinit var sharedPreferences : SharedPreferences

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        contextMain = container!!.context
        dialog = contextMain.createProgressDialog()
        cartViewModel = (contextMain as HomeActivity).cartViewModel
        homeViewModel = activity?.let { ViewModelProvider(it).get(HomeViewModel::class.java) }!!

        v = inflater.inflate(R.layout.fragment_medicine, container, false)

        sharedPreferences = contextMain.getSharedPreferences("medicine_data",
                Context.MODE_PRIVATE
        )

        (v.context as HomeActivity).titleActionBarTextView.text = getString(R.string.medicine_page_title)
        (v.context as HomeActivity).deleteItemsFromCart.visibility = View.GONE
        (v.context as HomeActivity).img_cart_icon.visibility = View.VISIBLE

        v.txt_pharmacy.setText(sharedPreferences.getString("txt_pharmacy",""))
        v.txt_details.setText(sharedPreferences.getString("txt_details",""))
        imagePath = Uri.parse(sharedPreferences.getString("photo",""))
        if(imagePath.toString().isNotEmpty()){
            v.imageView.setImageURI(imagePath)
        }
        v.txt_pharmacy.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                sharedPreferences.edit().putString("txt_pharmacy",s.toString())
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
        v.txt_details.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                sharedPreferences.edit().putString("txt_details",s.toString())
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

        v.imageView.setOnClickListener {
//          val choose = Intent(Intent.ACTION_PICK,
//                  MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//          startActivityForResult(choose, PICK_IMAGE_CODE)

            ImagePicker.with(this)
                    .crop()
                    .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
                    .start()
        }
        v.txt_add_to_cart.setOnClickListener {
            if (imagePath.toString().isNotEmpty() ||
                    v.txt_details.text.isNotEmpty()
            ) {
                checkOrderStatus(v.txt_pharmacy.text.toString(),v.txt_details.text.toString(), imagePath.toString())
            } else {
                FancyToast.makeText(
                        context, getString(R.string.fill_all_the_fields),
                        FancyToast.LENGTH_SHORT, FancyToast.ERROR, false
                ).show()
            }
        }
        return v
    }

    private fun checkOrderStatus(name: String, details: String, image: String) {
        dialog.show()
        val limit = homeViewModel.getCategoriesMaxOrderLimitMediciner()
        val size = (contextMain as HomeActivity).cartItemsAllMainList.filter { cartProductEntity -> cartProductEntity.medicine_item }.size
        val size_all_categories = (contextMain as HomeActivity).cartItemsAllMainList.filter { cartProductEntity -> !cartProductEntity.product_item  }.size
        val limit_all_categories = homeViewModel.getCategoriesMaxOrderLimit()

        if(size < limit && size_all_categories < limit_all_categories){
            placeOrder(name, details, image)
        }else{
            FancyToast.makeText(
                    context, getString(R.string.you_have_reached_the_maximum_limit_orders),
                    FancyToast.LENGTH_SHORT, FancyToast.ERROR, false
            ).show()
            dialog.dismiss()
        }
    }

    private fun placeOrder(name: String, details: String, image: String) {
        val cartProductEntity = CartProductEntity()
        cartProductEntity.medicine_item = true
        cartProductEntity.medicine_order_text = name
        cartProductEntity.medicine_order_text_2 = details
        cartProductEntity.medicine_order_image = image
        cartViewModel.insertItemToCart(contextMain, cartProductEntity)
        dialog.dismiss()
        FancyToast.makeText(
                context, getString(R.string.added_to_cart_successfully),
                FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false
        ).show()
        sharedPreferences.edit().clear().apply()
        (contextMain as HomeActivity).onBackPressed()
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