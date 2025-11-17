package com.easy.simplebudgetplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.easy.simplebudgetplanner.data.BlueprintDatabase
import com.easy.simplebudgetplanner.data.BlueprintRepository
import com.easy.simplebudgetplanner.data.CycleLedger
import com.easy.simplebudgetplanner.data.PlanCycle
import com.easy.simplebudgetplanner.data.PlanTransaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BoardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BlueprintRepository(
        BlueprintDatabase.get(application).cycleDao(),
        BlueprintDatabase.get(application).transactionDao()
    )

    val cycles: StateFlow<List<PlanCycle>> =
        repository.observeCycles()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun createQuickCycle(label: String, income: Double) {
        viewModelScope.launch {
            repository.createCycle(label = label, income = income)
        }
    }

    fun createSpecificCycle(label: String, year: Int, month: Int, income: Double) {
        viewModelScope.launch {
            repository.createCycle(label = label, year = year, month = month, income = income)
        }
    }

    fun updateCycle(cycle: PlanCycle) {
        viewModelScope.launch {
            repository.updateCycle(cycle)
        }
    }

    fun deleteCycle(cycle: PlanCycle) {
        viewModelScope.launch {
            repository.deleteCycle(cycle)
        }
    }

    suspend fun snapshotTotals(): List<Pair<PlanCycle, Double>> = repository.snapshotTotals()
}

class PeriodViewModel(
    application: Application,
    private val cycleId: Long
) : AndroidViewModel(application) {

    private val repository = BlueprintRepository(
        BlueprintDatabase.get(application).cycleDao(),
        BlueprintDatabase.get(application).transactionDao()
    )

    val ledger: StateFlow<CycleLedger?> =
        repository.observeLedger(cycleId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    fun adjustIncome(income: Double) {
        viewModelScope.launch {
            repository.getCycle(cycleId)?.let {
                repository.updateCycle(it.copy(income = income))
            }
        }
    }

    fun updateCycleMeta(label: String, year: Int, month: Int) {
        viewModelScope.launch {
            repository.getCycle(cycleId)?.let {
                repository.updateCycle(
                    it.copy(
                        label = label.trim(),
                        year = year,
                        month = month
                    )
                )
            }
        }
    }

    fun addTransaction(title: String, amount: Double, category: String, millis: Long) {
        viewModelScope.launch {
            repository.addTransaction(
                cycleId = cycleId,
                title = title,
                amount = amount,
                category = category,
                millis = millis
            )
        }
    }

    fun updateTransaction(transaction: PlanTransaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: PlanTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}

class PeriodViewModelFactory(
    private val application: Application,
    private val cycleId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PeriodViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PeriodViewModel(application, cycleId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}



