package arpan.delivery.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import arpan.delivery.R
import arpan.delivery.data.adapters.OfferItemRecyclerAdapter
import arpan.delivery.data.models.OfferImage
import arpan.delivery.utils.Constants
import kotlinx.android.synthetic.main.fragment_offers.view.*
import java.util.*
import kotlin.collections.ArrayList

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [OffersFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OffersFragment : Fragment() {
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_offers, container, false)
        (view.context as HomeActivity).homeViewModel.getOffersDocumentSnapshotMainData().observe(viewLifecycleOwner, androidx.lifecycle.Observer {
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
                val linearLayoutManager = LinearLayoutManager(view.context)
                linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
                view.mainRecyclerView.layoutManager = linearLayoutManager
                view.mainRecyclerView.adapter = OfferItemRecyclerAdapter(view.context, imagesList)
            }
        })
        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment OffersFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            OffersFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}