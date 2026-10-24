package eu.pascariu.shoppinglist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMutationDao {

    @Insert
    suspend fun enqueue(mutation: PendingMutation): Long

    @Query("SELECT * FROM pending_mutations ORDER BY id ASC LIMIT 1")
    suspend fun peekOldest(): PendingMutation?

    @Query("SELECT * FROM pending_mutations ORDER BY id DESC LIMIT 1")
    suspend fun peekNewest(): PendingMutation?

    @Query("SELECT * FROM pending_mutations ORDER BY id ASC")
    suspend fun listAll(): List<PendingMutation>

    @Query("SELECT COUNT(*) FROM pending_mutations")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM pending_mutations WHERE id = :mutationId")
    suspend fun deleteById(mutationId: Long)

    @Query("UPDATE pending_mutations SET payloadJson = :payloadJson WHERE id = :mutationId")
    suspend fun updatePayload(mutationId: Long, payloadJson: String)

    @Query("DELETE FROM pending_mutations")
    suspend fun clear()
}
