package com.munch.project.map

import android.app.Application
import androidx.room.*
import com.munch.lib.AppHelper
import com.munch.lib.amap.Location
import com.munch.lib.extend.SingletonHolder
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tb_sport_id")
data class SportIdRecord(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    var count: Int = 0,
    @ColumnInfo(name = "s_time")
    var startTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "e_time")
    var endTime: Long = 0
)

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

    @Query("SELECT COUNT(*) FROM tb_address WHERE :id = id")
    fun queryAddressCountBy(id: Int): Int

    @Query("DELETE FROM tb_address WHERE :id = id")
    suspend fun delAddressById(id: Int)

    @Query("DELETE FROM tb_sport_id WHERE :id = id")
    suspend fun delSportId(id: Int)

    @Transaction
    suspend fun del(id: Int) {
        delAddressById(id)
        delSportId(id)
    }

    @Insert
    suspend fun add(record: AddressRecord)

    @Query("SELECT MAX(id) FROM tb_sport_id")
    suspend fun getSportID(): Int

    @Query("SELECT * FROM tb_sport_id ORDER BY id DESC")
    suspend fun querySportId(): List<SportIdRecord>

    @Query("SELECT count(*) FROM tb_sport_id")
    fun getSportIDCount(): Flow<Int>

    @Insert
    suspend fun addSport(sport: SportIdRecord)

    @Query("SELECT * from tb_sport_id where id == :id")
    suspend fun getSportIdById(id: Int): SportIdRecord?

    @Update
    suspend fun updateSport(sport: SportIdRecord)
}

@Database(
    entities = [AddressRecord::class, SportIdRecord::class],
    version = AddressDB.VERSION_22_07_12,
    exportSchema = true
)
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

object Record : AddressDao by AddressDB.getInstance(AppHelper.app).record

