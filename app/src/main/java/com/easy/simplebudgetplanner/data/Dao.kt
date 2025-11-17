package com.easy.simplebudgetplanner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanCycleDao {
    @Query("SELECT * FROM plan_cycles ORDER BY year DESC, month DESC")
    fun watchAll(): Flow<List<PlanCycle>>

    @Query("SELECT * FROM plan_cycles ORDER BY year DESC, month DESC")
    suspend fun fetchAll(): List<PlanCycle>

    @Query("SELECT * FROM plan_cycles WHERE id = :id")
    fun watchOne(id: Long): Flow<PlanCycle?>

    @Query("SELECT * FROM plan_cycles WHERE id = :id")
    suspend fun findOne(id: Long): PlanCycle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cycle: PlanCycle): Long

    @Update
    suspend fun update(cycle: PlanCycle)

    @Delete
    suspend fun delete(cycle: PlanCycle)
}

@Dao
interface PlanTransactionDao {
    @Query("SELECT * FROM plan_transactions WHERE cycle_id = :cycleId ORDER BY spent_at DESC, id DESC")
    fun watchForCycle(cycleId: Long): Flow<List<PlanTransaction>>

    @Query("SELECT * FROM plan_transactions WHERE cycle_id = :cycleId ORDER BY spent_at DESC, id DESC")
    suspend fun fetchForCycle(cycleId: Long): List<PlanTransaction>

    @Query("SELECT SUM(amount) FROM plan_transactions WHERE cycle_id = :cycleId")
    fun watchTotal(cycleId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM plan_transactions WHERE cycle_id = :cycleId")
    suspend fun fetchTotal(cycleId: Long): Double?

    @Query("SELECT * FROM plan_transactions WHERE id = :id")
    suspend fun findOne(id: Long): PlanTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: PlanTransaction): Long

    @Update
    suspend fun update(transaction: PlanTransaction)

    @Delete
    suspend fun delete(transaction: PlanTransaction)
}



