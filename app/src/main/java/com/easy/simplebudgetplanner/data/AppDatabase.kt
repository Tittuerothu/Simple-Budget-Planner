package com.easy.simplebudgetplanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlanCycle::class, PlanTransaction::class],
    version = 1,
    exportSchema = false
)
abstract class BlueprintDatabase : RoomDatabase() {
    abstract fun cycleDao(): PlanCycleDao
    abstract fun transactionDao(): PlanTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: BlueprintDatabase? = null

        fun get(context: Context): BlueprintDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BlueprintDatabase::class.java,
                    "blueprint-ledger.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}



