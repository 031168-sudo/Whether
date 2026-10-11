package ru.andrey.whether.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import ru.andrey.whether.data.DayRecord

/**
 * Суточная запись в кэше. Ключ — округлённые координаты (locKey) и дата.
 * Округление до 0.01° даёт точность около километра, этого хватает для погоды.
 */
@Entity(tableName = "daily", primaryKeys = ["locKey", "date"])
data class DailyEntity(
    val locKey: String,
    val date: String,
    val tMax: Double?,
    val tMin: Double?,
    val tMean: Double?,
    val precipitation: Double?,
    val windMax: Double?,
    val humidityMean: Double?
)

@Dao
interface DailyDao {

    @Query("SELECT * FROM daily WHERE locKey = :key AND date BETWEEN :start AND :end ORDER BY date")
    suspend fun range(key: String, start: String, end: String): List<DailyEntity>

    @Upsert
    suspend fun upsertAll(items: List<DailyEntity>)
}

@Database(entities = [DailyEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun daily(): DailyDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "whether.db")
                .build()
    }
}

fun DayRecord.toEntity(locKey: String) = DailyEntity(
    locKey = locKey,
    date = date,
    tMax = tMax,
    tMin = tMin,
    tMean = tMean,
    precipitation = precipitation,
    windMax = windMax,
    humidityMean = humidityMean
)

fun DailyEntity.toRecord() = DayRecord(
    date = date,
    tMax = tMax,
    tMin = tMin,
    tMean = tMean,
    precipitation = precipitation,
    windMax = windMax,
    humidityMean = humidityMean
)
