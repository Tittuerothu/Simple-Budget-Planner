package com.easy.simplebudgetplanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easy.simplebudgetplanner.screens.BlueprintPeriodScreen
import com.easy.simplebudgetplanner.ui.theme.SimpleBudgetPlannerTheme
import com.easy.simplebudgetplanner.viewmodel.PeriodViewModel
import com.easy.simplebudgetplanner.viewmodel.PeriodViewModelFactory

class PlanDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CYCLE_ID = "cycle_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cycleId = intent.getLongExtra(EXTRA_CYCLE_ID, -1L)
        if (cycleId == -1L) {
            finish()
            return
        }

        val periodViewModel: PeriodViewModel by viewModels {
            PeriodViewModelFactory(application, cycleId)
        }

        setContent {
            SimpleBudgetPlannerTheme {
                val ledger by periodViewModel.ledger.collectAsStateWithLifecycle()

                BlueprintPeriodScreen(
                    ledger = ledger,
                    onNavigateUp = { onBackPressedDispatcher.onBackPressed() },
                    onEditIncome = { income -> periodViewModel.adjustIncome(income) },
                    onUpdateCycle = { label, year, month ->
                        periodViewModel.updateCycleMeta(label, year, month)
                    },
                    onAddTransaction = { title, amount, category, millis ->
                        periodViewModel.addTransaction(title, amount, category, millis)
                    },
                    onUpdateTransaction = { transaction ->
                        periodViewModel.updateTransaction(transaction)
                    },
                    onDeleteTransaction = { transaction ->
                        periodViewModel.deleteTransaction(transaction)
                    },
                    onOpenInsights = {
                        startActivity(Intent(this, InsightActivity::class.java))
                    }
                )
            }
        }
    }
}




