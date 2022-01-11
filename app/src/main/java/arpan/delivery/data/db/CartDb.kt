package arpan.delivery.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CartProductEntity::class], version = 1,  exportSchema = true)
abstract class CartDb : RoomDatabase() {
    abstract val cartDao : CartInterface
    companion object {
        @Volatile
        private var INSTANCE : CartDb? = null
        fun getInstance(context : Context): CartDb {
            synchronized(this){
                var instance = INSTANCE
                if(instance == null){
                    instance = Room.databaseBuilder(context,
                        CartDb::class.java,
                        "cart_data_database"
                    ).build()
                }
                return instance
            }
        }
    }
}