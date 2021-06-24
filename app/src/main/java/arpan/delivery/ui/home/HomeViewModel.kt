package arpan.delivery.ui.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arpan.delivery.R
import arpan.delivery.data.models.LocationItem
import arpan.delivery.data.models.PromoCode
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.shreyaspatil.MaterialDialog.MaterialDialog

class HomeViewModel : ViewModel() {

    private var dataLoaded = MutableLiveData(false)

    fun setStatus(status : Boolean){
        dataLoaded = MutableLiveData(status)
    }

    fun dataLoaded() : MutableLiveData<Boolean> {
        return dataLoaded
    }

    private var maxShops = MutableLiveData(1)
    private var allowMoreShops = MutableLiveData(false)
    private var deliveryChargeExtra = MutableLiveData(20)
    private var daChargeExtra = MutableLiveData(10)

    fun setMaxShops(i : Int){
        maxShops = MutableLiveData(i)
    }
    fun setAllowMoreShops(i : Boolean){
        allowMoreShops = MutableLiveData(i)
    }
    fun setDeliveryChargeExtra(i : Int){
        deliveryChargeExtra = MutableLiveData(i)
    }
    fun setDAChargeExtra(i : Int){
        daChargeExtra = MutableLiveData(i)
    }

    fun getMaxShops() : MutableLiveData<Int>{
        return maxShops
    }
    fun getAllowMoreShops() : MutableLiveData<Boolean>{
        return allowMoreShops
    }
    fun getDeliveryChargeExtra() : MutableLiveData<Int>{
        return deliveryChargeExtra
    }
    fun getDAChargeExtra() : MutableLiveData<Int>{
        return daChargeExtra
    }

    private lateinit var offersDocumentSnapshotMain : MutableLiveData<Task<DocumentSnapshot>>
    private lateinit var offersDocumentSnapshotMainData : MutableLiveData<Task<DocumentSnapshot>>
    private lateinit var categoriesDocumentSnapshotMain : MutableLiveData<Task<DocumentSnapshot>>

    private var categoriesMaxOrderLimitCustomOrder : MutableLiveData<Int> = MutableLiveData(0)
    private var categoriesMaxOrderLimitParcel : MutableLiveData<Int> = MutableLiveData(0)
    private var categoriesMaxOrderLimitMedicine: MutableLiveData<Int> = MutableLiveData(0)
    private var categoriesMaxOrderLimit: MutableLiveData<Int> = MutableLiveData(0)

    private var locationsArrayNormal : MutableLiveData<ArrayList<LocationItem>> = MutableLiveData(ArrayList())
    private var locationsArrayPickDrop : MutableLiveData<ArrayList<LocationItem>> = MutableLiveData(ArrayList())

    fun getLocationArray() : ArrayList<LocationItem>{
        return locationsArrayNormal.value!!
    }
    fun getLocationArrayPickDrop() : ArrayList<LocationItem>{
        return locationsArrayPickDrop.value!!
    }
    fun setLocationArray(a : ArrayList<LocationItem>){
        locationsArrayNormal = MutableLiveData(a)
    }
    fun setLocationArrayPickDrop(a : ArrayList<LocationItem>){
        locationsArrayPickDrop = MutableLiveData(a)
    }

    fun setOffersDocumentSnapshotData(documentSnapshotTask : MutableLiveData<Task<DocumentSnapshot>>){
        offersDocumentSnapshotMain = documentSnapshotTask
    }

    fun setOffersDocumentSnapshotMainData(documentSnapshotTask : MutableLiveData<Task<DocumentSnapshot>>){
        offersDocumentSnapshotMainData = documentSnapshotTask
    }

    fun setCategoriesDocumentSnapshotData(documentSnapshotTask : MutableLiveData<Task<DocumentSnapshot>>){
        categoriesDocumentSnapshotMain = documentSnapshotTask
    }

    fun getOffersDocumentSnapshotData(): MutableLiveData<Task<DocumentSnapshot>> {
        return offersDocumentSnapshotMain
    }

    fun getOffersDocumentSnapshotMainData(): MutableLiveData<Task<DocumentSnapshot>> {
        return offersDocumentSnapshotMainData
    }

    fun getCategoriesDocumentSnapshotData(): MutableLiveData<Task<DocumentSnapshot>> {
        return categoriesDocumentSnapshotMain
    }

    fun setCategoriesMaxOrderLimitCustomOrder(i : Int){
        categoriesMaxOrderLimitCustomOrder = MutableLiveData(i)
    }
    fun setCategoriesMaxOrderLimitParcel(i : Int){
        categoriesMaxOrderLimitParcel = MutableLiveData(i)
    }
    fun setCategoriesMaxOrderLimitMedicine(i : Int){
        categoriesMaxOrderLimitMedicine = MutableLiveData(i)
    }
    fun getCategoriesMaxOrderLimitCustomOrder() : Int{
        return categoriesMaxOrderLimitCustomOrder.value!!
    }
    fun getCategoriesMaxOrderLimitParcel() : Int{
        return categoriesMaxOrderLimitParcel.value!!
    }
    fun getCategoriesMaxOrderLimitMediciner() : Int{
        return categoriesMaxOrderLimitMedicine.value!!
    }
    fun getCategoriesMaxOrderLimit() : Int{
        return categoriesMaxOrderLimit.value!!
    }
    fun setCategoriesMaxOrderLimit(i : Int){
        categoriesMaxOrderLimit = MutableLiveData(i)
    }

    fun callPermissionCheck(context: Context, activity: Activity): Boolean {
        return if(ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.CALL_PHONE
                    )
            ) {
                val mDialog = MaterialDialog.Builder(activity)
                        .setTitle(context.getString(R.string.give_call_permission))
                        .setMessage(context.getString(R.string.call_permission_for_first_time))
                        .setCancelable(false)
                        .setPositiveButton(
                                context.getString(R.string.yes_ok), R.drawable.ic_delete
                        ) { diaInt, _ ->
                            ActivityCompat.requestPermissions(
                                    activity,
                                    arrayOf(Manifest.permission.CALL_PHONE),
                                    0
                            )
                            diaInt.dismiss()
                        }
                        .setNegativeButton(
                                context.getString(R.string.no_its_ok),
                                R.drawable.ic_clear
                        ) { dialogInterface, _ -> dialogInterface.dismiss() }
                        .build()
                mDialog.show()
            } else {
                ActivityCompat.requestPermissions(activity,arrayOf(Manifest.permission.CALL_PHONE),
                        0)
            }
            false
        } else {
            true
        }
    }

    private lateinit var timeBasedNotificationsDocumentSnapshotMainData :  MutableLiveData<Task<DocumentSnapshot>>
    private lateinit var normalNotificationsDocumentSnapshotMainData :  MutableLiveData<Task<DocumentSnapshot>>

    fun setTimeBasedNotificationsDocumentSnapshotMainData(mutableLiveData: MutableLiveData<Task<DocumentSnapshot>>) {
        timeBasedNotificationsDocumentSnapshotMainData  = mutableLiveData
    }

    fun setNormalNotificationsDocumentSnapshotMainData(mutableLiveData: MutableLiveData<Task<DocumentSnapshot>>) {
        normalNotificationsDocumentSnapshotMainData = mutableLiveData
    }
    fun getTimeBasedNotificationsDocumentSnapshotMainData() : MutableLiveData<Task<DocumentSnapshot>>{
        return timeBasedNotificationsDocumentSnapshotMainData
    }

    fun getNormalNotificationsDocumentSnapshotMainData(): MutableLiveData<Task<DocumentSnapshot>> {
        return normalNotificationsDocumentSnapshotMainData
    }

}