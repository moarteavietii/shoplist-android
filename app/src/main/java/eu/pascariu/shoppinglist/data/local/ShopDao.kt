package eu.pascariu.shoppinglist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {

    @Query("SELECT * FROM shops ORDER BY id ASC")
    fun observeAll(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE id = :shopId")
    suspend fun getById(shopId: Long): ShopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(shops: List<ShopEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shop: ShopEntity)

    @Query("DELETE FROM shops WHERE id = :shopId")
    suspend fun deleteById(shopId: Long)

    @Query("DELETE FROM shops")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(shops: List<ShopEntity>) {
        clear()
        upsertAll(shops)
    }
}
