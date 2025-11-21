package com.easy.simplebudgetplanner

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Shared helper functions used across multiple screens in the SimpleBudgetPlanner app.
 * This file consolidates common formatting and utility functions to avoid duplication.
 */

/**
 * Currency formatter that uses Euro (€) symbol.
 * Returns a NumberFormat instance configured for German locale (Euro currency).
 */
val currencyFormatter: NumberFormat
    get() = NumberFormat.getCurrencyInstance(Locale.GERMANY) // Uses Euro (€) symbol

/**
 * Converts a month number (1-12) to its full month name.
 * @param month The month number (1 = January, 12 = December)
 * @return The full month name (e.g., "January", "February")
 */
fun monthLabel(month: Int): String {
    val calendar = Calendar.getInstance().apply { set(Calendar.MONTH, month - 1) }
    return SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
}

/**
 * Formats a date in a friendly format (e.g., "Jan 15, 2024").
 * Used for displaying expense dates.
 * @param millis The timestamp in milliseconds since epoch
 * @return A formatted date string (e.g., "Jan 15, 2024")
 */
fun expenseDate(millis: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(millis)
}

/**
 * Generates an auto-label for the current month and year.
 * Used as a default label when creating new cycles.
 * @return A formatted string (e.g., "January 2024")
 */
fun autoLabel(): String {
    val now = Calendar.getInstance()
    val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return format.format(now.time)
}

