package com.easy.simplebudgetplanner.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

data class CycleLedger(
    val cycle: PlanCycle,
    val transactions: List<PlanTransaction>,
    val totalSpent: Double,
    val balance: Double
)

class BlueprintRepository(
    private val cycleDao: PlanCycleDao,
    private val transactionDao: PlanTransactionDao
) {
    fun observeCycles(): Flow<List<PlanCycle>> = cycleDao.watchAll()

    fun observeLedger(cycleId: Long): Flow<CycleLedger?> {
        val cycleFlow = cycleDao.watchOne(cycleId)
        val txFlow = transactionDao.watchForCycle(cycleId)
        val totalFlow = transactionDao.watchTotal(cycleId).map { it ?: 0.0 }
        return combine(cycleFlow, txFlow, totalFlow) { cycle, transactions, total ->
            cycle?.let {
                CycleLedger(
                    cycle = it,
                    transactions = transactions,
                    totalSpent = total,
                    balance = it.income - total
                )
            }
        }
    }

    suspend fun createCycle(
        label: String,
        year: Int? = null,
        month: Int? = null,
        income: Double = 0.0
    ): Long = withContext(Dispatchers.IO) {
        val now = Calendar.getInstance()
        val resolvedYear = year ?: now.get(Calendar.YEAR)
        val resolvedMonth = month ?: (now.get(Calendar.MONTH) + 1)
        val cycle = PlanCycle(
            label = label.trim(),
            year = resolvedYear,
            month = resolvedMonth,
            income = income
        )
        cycleDao.insert(cycle)
    }

    suspend fun updateCycle(cycle: PlanCycle) = withContext(Dispatchers.IO) {
        cycleDao.update(cycle)
    }

    suspend fun deleteCycle(cycle: PlanCycle) = withContext(Dispatchers.IO) {
        cycleDao.delete(cycle)
    }

    suspend fun getCycle(id: Long): PlanCycle? = withContext(Dispatchers.IO) {
        cycleDao.findOne(id)
    }

    suspend fun addTransaction(
        cycleId: Long,
        title: String,
        amount: Double,
        category: String,
        millis: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val tx = PlanTransaction(
            cycleId = cycleId,
            title = title.trim(),
            amount = amount,
            category = category.trim(),
            spentAt = millis
        )
        transactionDao.insert(tx)
    }

    suspend fun updateTransaction(transaction: PlanTransaction) = withContext(Dispatchers.IO) {
        transactionDao.update(transaction)
    }

    suspend fun deleteTransaction(transaction: PlanTransaction) = withContext(Dispatchers.IO) {
        transactionDao.delete(transaction)
    }

    suspend fun getTransaction(id: Long): PlanTransaction? = withContext(Dispatchers.IO) {
        transactionDao.findOne(id)
    }

    suspend fun snapshotTotals(): List<Pair<PlanCycle, Double>> = withContext(Dispatchers.IO) {
        val cycles = cycleDao.fetchAll()
        cycles.map { it to (transactionDao.fetchTotal(it.id) ?: 0.0) }
    }
}



