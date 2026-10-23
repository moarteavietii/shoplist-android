package eu.pascariu.shoppinglist.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UndoHistoryDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ListPreferencesDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ExpiryTicker
