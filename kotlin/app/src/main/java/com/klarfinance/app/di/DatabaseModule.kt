package com.klarfinance.app.di

import android.content.Context
import androidx.room.Room
import com.klarfinance.app.data.local.KlarFinanceDatabase
import com.klarfinance.app.data.local.VerifiedPhoneDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KlarFinanceDatabase =
        Room.databaseBuilder(context, KlarFinanceDatabase::class.java, "klarfinance.db").build()

    @Provides
    fun provideVerifiedPhoneDao(database: KlarFinanceDatabase): VerifiedPhoneDao = database.verifiedPhoneDao()
}
