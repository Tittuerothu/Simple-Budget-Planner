package com.easy.simplebudgetplanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easy.simplebudgetplanner.screens.BoardUiState
import com.easy.simplebudgetplanner.screens.BlueprintBoardScreen
import com.easy.simplebudgetplanner.screens.buildBoardUiState
import com.easy.simplebudgetplanner.ui.theme.SimpleBudgetPlannerTheme
import com.easy.simplebudgetplanner.viewmodel.BoardViewModel

class MainActivity : ComponentActivity() {

    private val boardViewModel: BoardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimpleBudgetPlannerTheme {
                val cycles by boardViewModel.cycles.collectAsStateWithLifecycle()
                val state: BoardUiState = remember(cycles) {
                    buildBoardUiState(cycles)
                }
                val context = LocalContext.current

                BlueprintBoardScreen(
                    state = state,
                    onCreateQuick = { title, income ->
                        boardViewModel.createQuickCycle(title, income)
                    },
                    onCreateCustom = { title, year, month, income ->
                        boardViewModel.createSpecificCycle(title, year, month, income)
                    },
                    onOpenCycle = { id ->
                        context.startActivity(
                            Intent(context, PlanDetailActivity::class.java)
                                .putExtra(PlanDetailActivity.EXTRA_CYCLE_ID, id)
                        )
                    },
                    onEditCycle = { cycle ->
                        boardViewModel.updateCycle(cycle)
                    },
                    onDeleteCycle = { cycle ->
                        boardViewModel.deleteCycle(cycle)
                    },
                    onOpenInsights = {
                        context.startActivity(Intent(context, InsightActivity::class.java))
                    }
                )
            }
        }
    }
}
