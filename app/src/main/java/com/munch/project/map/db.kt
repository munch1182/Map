package com.munch.project.map

import android.app.Application
import androidx.room.*
import com.munch.lib.AppHelper
import com.munch.lib.amap.Location
import com.munch.lib.extend.SingletonHolder
import com.munch.lib.helper.data.SpHelper
import com.munch.lib.record.Record
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tb_address")
data class AddressRecord(
    @ColumnInfo(name = "id")
    var sportId: Int,
    @ColumnInfo(name = "la")
    var latitude: Double,
    // 经度
    @ColumnInfo(name = "lng")
    var longitude: Double,
    // 海拔
    @ColumnInfo(name = "alt")
    var altitude: Double,
    //
    @PrimaryKey(autoGenerate = false)
    val time: Long = System.currentTimeMillis()
) {

    companion object {

        fun Location.into(sportId: Int): AddressRecord {
            return AddressRecord(
                sportId,
                this.laLng!!.latitude,
                this.laLng!!.longitude,
                this.laLng!!.altitude
            )
        }
    }
}

@Dao
interface AddressDao {

    @Query("SELECT * FROM tb_address WHERE :id = id")
    suspend fun queryAllById(id: Int): List<AddressRecord>

    @Query("SELECT * FROM tb_address WHERE :id = id")
    fun queryAllFlowById(id: Int): Flow<AddressRecord>

    @Query("DELETE FROM tb_address WHERE :id = id")
    suspend fun del(id: Int)

    @Insert
    suspend fun add(record: AddressRecord)
}

@Database(entities = [AddressRecord::class], version = AddressDB.VERSION_22_07_12, exportSchema = true)
abstract class AddressDatabase : RoomDatabase() {

    abstract fun addressDao(): AddressDao
}

class AddressDB private constructor(app: Application) {

    companion object : SingletonHolder<AddressDB, Application>({ AddressDB(it) }) {
        internal const val NAME_DB = "db_address"

        internal const val VERSION_22_07_12 = 1
    }

    private val db = Room.databaseBuilder(app, AddressDatabase::class.java, NAME_DB)
        /*.setQueryCallback(
            { sqlQuery, bindArgs -> log(sqlQuery, bindArgs) },
            ThreadHelper.newCachePool()
        )*/
        .build()

    val record: AddressDao = db.addressDao()
}

object Record : AddressDao by AddressDB.getInstance(AppHelper.app).record {

    private const val KEY_ID = "KEY_ID"

    fun getSportID(): Int {
        return SpHelper.getSp().get(KEY_ID, 0) ?: 0
    }

    fun nextSportID() {
        SpHelper.getSp().plus(KEY_ID, 1, 0)
    }
}

