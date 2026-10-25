package eu.pascariu.shoppinglist.ui.list

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import eu.pascariu.shoppinglist.R
import javax.inject.Inject
import javax.inject.Singleton

interface ListStringProvider {
    val refreshFailure: String
    val moveFailure: String
    val saveFailure: String
    val deleteFailure: String
    val manualSortCheckedItemsNotice: String
}

@Singleton
class AndroidListStringProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : ListStringProvider {
    override val refreshFailure: String
        get() = context.getString(R.string.toast_refresh_failure)
    override val moveFailure: String
        get() = context.getString(R.string.toast_move_failure)
    override val saveFailure: String
        get() = context.getString(R.string.toast_save_failure)
    override val deleteFailure: String
        get() = context.getString(R.string.toast_delete_failure)
    override val manualSortCheckedItemsNotice: String
        get() = context.getString(R.string.toast_manual_sort_checked_items)
}

@Module
@InstallIn(SingletonComponent::class)
object ListStringModule {

    @Provides
    @Singleton
    fun provideListStringProvider(impl: AndroidListStringProvider): ListStringProvider = impl
}
