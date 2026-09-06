package com.klarfinance.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [VerifiedPhoneEntity::class], version = 1, exportSchema = false)
abstract class KlarFinanceDatabase : RoomDatabase() {
    abstract fun verifiedPhoneDao(): VerifiedPhoneDao
}
