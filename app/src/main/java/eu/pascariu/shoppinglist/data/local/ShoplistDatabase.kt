package eu.pascariu.shoppinglist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class MutationKindConverter {
    @TypeConverter
    fun fromKind(kind: MutationKind): String = kind.name

    @TypeConverter
    fun toKind(value: String): MutationKind = MutationKind.valueOf(value)
}

@Database(
    entities = [ItemEntity::class, ShopEntity::class, PendingMutation::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(MutationKindConverter::class)
abstract class ShoplistDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao

    abstract fun shopDao(): ShopDao

    abstract fun pendingMutationDao(): PendingMutationDao
}
