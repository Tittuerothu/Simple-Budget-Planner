package com.easy.simplebudgetplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easy.simplebudgetplanner.data.PlanCycle
import com.easy.simplebudgetplanner.screens.BlueprintInsightScreen
import com.easy.simplebudgetplanner.ui.theme.SimpleBudgetPlannerTheme
import com.easy.simplebudgetplanner.viewmodel.BoardViewModel

class InsightActivity : ComponentActivity() {

    private val boardViewModel: BoardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimpleBudgetPlannerTheme {
                val cycles by boardViewModel.cycles.collectAsStateWithLifecycle()
                val snapshot by produceState(
                    initialValue = emptyList<Pair<PlanCycle, Double>>(),
                    key1 = cycles
                ) {
                    value = boardViewModel.snapshotTotals()
                }

                BlueprintInsightScreen(
                    data = snapshot,
                    onBack = { onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }
}




