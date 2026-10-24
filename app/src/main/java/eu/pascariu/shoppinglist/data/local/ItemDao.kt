package eu.pascariu.shoppinglist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items ORDER BY id ASC")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getById(itemId: Long): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteById(itemId: Long)

    @Query("DELETE FROM items")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<ItemEntity>) {
        clear()
        upsertAll(items)
    }
}
