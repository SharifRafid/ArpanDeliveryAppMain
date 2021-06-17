package arpan.delivery.ui.fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import arpan.delivery.R
import arpan.delivery.ui.home.HomeActivity
import arpan.delivery.ui.home.HomeViewModel
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.call_dialog_view.view.*
import kotlinx.android.synthetic.main.fragment_be_client.view.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BeClient.newInstance] factory method to
 * create an instance of this fragment.
 */
class BeClient : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_be_client, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            (view.context as HomeActivity).window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            (view.context as HomeActivity).window.statusBarColor = resources.getColor(R.color.blue_normal);
        }

        (view.context as HomeActivity).titleActionBarTextView.text = getString(R.string.client_be_title)
        (view.context as HomeActivity).deleteItemsFromCart.visibility = View.GONE
        (view.context as HomeActivity).img_cart_icon.visibility = View.VISIBLE

        val myCustomFont : Typeface? = ResourcesCompat.getFont(view.context, R.font.hind_siliguri_font)

        view.phjustifiedTextView.text = resources.getString(R.string.client_package)
        view.phjustifiedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,16F)
        //phjustifiedTextView.setLineSpacing(15)
        // Left for English - Right for Persian
        view.phjustifiedTextView.alignment = Paint.Align.LEFT
        view.phjustifiedTextView.typeFace = myCustomFont
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BeClient.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                BeClient().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}