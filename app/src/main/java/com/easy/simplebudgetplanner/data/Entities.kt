package com.easy.simplebudgetplanner.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a budgeting cycle in the SimpleBudgetPlanner app.
 */
@Entity(
    tableName = "plan_cycles",
    indices = [Index(value = ["year", "month"], unique = true)]
)
data class PlanCycle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String = "",
    val year: Int,
    val month: Int,
    val income: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

/**
 * Represents a single expense within a budgeting cycle.
 */
@Entity(
    tableName = "plan_transactions",
    foreignKeys = [
        ForeignKey(
            entity = PlanCycle::class,
            parentColumns = ["id"],
            childColumns = ["cycle_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cycle_id"), Index("spent_at")]
)
data class PlanTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "cycle_id") val cycleId: Long,
    val title: String,
    val amount: Double,
    val category: String = "",
    @ColumnInfo(name = "spent_at") val spentAt: Long = System.currentTimeMillis()
)









