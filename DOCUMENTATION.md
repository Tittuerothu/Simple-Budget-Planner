# Blueprint Budget Technical Guide

Blueprint Budget is a Compose-only personal finance tool that lets users design “plan cycles”, log expenses, and monitor how spending tracks against monthly income goals. The codebase blends Room for persistence, a repository for business rules, coroutine-powered ViewModels, and a distinctly different UI from the other budget variants. This document walks through the architecture and core methods so future maintainers can quickly understand responsibilities without digging into every file.

---

## 1. Architecture overview

The app follows a unidirectional data flow:

```
UI → ViewModel → Repository → Room → Flow → ViewModel → UI
```

* **Data layer** – `PlanCycle` / `PlanTransaction` entities, DAOs, and the `BlueprintDatabase` singleton.
* **Repository layer** – `BlueprintRepository` orchestrates queries and enforces defaults.
* **View-model layer** – `BoardViewModel` and `PeriodViewModel` expose `StateFlow` for Compose.
* **Presentation layer** – `BlueprintBoardScreen`, `BlueprintPeriodScreen`, and `BlueprintInsightScreen` define the neon dune-themed experience.

All writes run on `Dispatchers.IO`, and Compose observes immutable `StateFlow` objects, ensuring screens stay consistent with underlying data.

---

## 2. Data model and entities

### `PlanCycle`
Represents a budgeting cycle. Fields:
- `label`: optional user-facing name.
- `year` / `month`: integers identifying the cycle (index on both for quick ordering).
- `income`: monthly income target.
- `createdAt`: timestamp defaults to current time (helps with chronology).

### `PlanTransaction`
Represents an expense. Fields:
- `cycleId`: foreign key to `PlanCycle` with `ON DELETE CASCADE`.
- `title`, `amount`, optional `category`.
- `spentAt`: epoch millis defaulting to “now”.

Foreign keys prevent orphan transactions, and indices on `cycle_id` / `spent_at` keep queries fast.

---

## 3. DAOs and queries

* `PlanCycleDao.watchAll()` returns a descending flow of all cycles; `fetchAll()` supports snapshot work (e.g., chart data).
* `watchOne(id)` / `findOne(id)` provide reactive and one-off access to a single cycle.
* `PlanTransactionDao.watchForCycle(cycleId)` streams expenses ordered by date; `watchTotal(cycleId)` and `fetchTotal(cycleId)` compute aggregates directly in SQL.
* CRUD methods handle inserts, updates, and deletes for both tables.

---

## 4. Repository orchestration

`BlueprintRepository` composes DAO results into a `CycleLedger` structure:

```kotlin
data class CycleLedger(
    val cycle: PlanCycle,
    val transactions: List<PlanTransaction>,
    val totalSpent: Double,
    val balance: Double
)
```

The repository:
- Defaults missing month/year to the current calendar during cycle creation.
- Trims labels/categories and stamps a current timestamp when dates aren’t provided.
- Collects totals via DAO aggregates, keeping Compose free of manual arithmetic.
- Provides `snapshotTotals()` for the insight chart.

All methods wrap DAO calls in `withContext(Dispatchers.IO)` to keep operations off the main thread.

---

## 5. ViewModels and state management

### `BoardViewModel`
* Builds the repository and exposes `cycles: StateFlow<List<PlanCycle>>`.
* Provides `createQuickCycle`, `createSpecificCycle`, `updateCycle`, `deleteCycle`.
* Supplies `snapshotTotals()` for the insight activity.

### `PeriodViewModel`
* Constructed via `PeriodViewModelFactory` with a cycle ID.
* `ledger: StateFlow<CycleLedger?>` streams the selected cycle, its transactions, totals, and balance.
* Offers methods to adjust income, update metadata, and add/update/delete transactions.

Both ViewModels use `viewModelScope.launch { … }`, ensuring coroutine work respects lifecycle scope.

---

## 6. Theme and design system

Blueprint Budget uses a bespoke dune-inspired palette:
- Light theme: warm sand backgrounds, sunset primaries, sage secondary accents.
- Dark theme: midnight blues with misty highlights.

Dynamic color is disabled to keep branding consistent. Elevated surfaces, typography hierarchy, and radial gradients distinguish hero widgets from transactional content. Status-bar colors mirror the active theme and toggle light icons automatically.

---

## 7. Screens and navigation

### `MainActivity`
Collects cycles from `BoardViewModel`, builds a `BoardUiState` summary, and renders `BlueprintBoardScreen`. Intent navigation targets:
- `PlanDetailActivity` (cycle detail).
- `InsightActivity` (multi-month chart).

### `PlanDetailActivity`
Creates `PeriodViewModel` via factory, collects `ledger`, and binds callbacks from `BlueprintPeriodScreen` to repository actions.

### `InsightActivity`
Reuses `BoardViewModel` to obtain a totals snapshot and renders `BlueprintInsightScreen`.

Manifest entries leave secondary activities internal (`exported="false"`), maintaining a clean launch flow.

---

## 8. BlueprintBoardScreen (Home)

Key UI features:
- **HaloHeader**: radial gradient card displaying total/average income with a “Plan custom cycle” action.
- **Timeline list**: each cycle card appears along a vertical connector with concentric dots, providing a different visual rhythm than prior apps.
- **Dialogs**:
  - `QuickCaptureDialog`: lightweight label/income entry.
  - `CycleComposerSheet`: bottom sheet with month/year selectors (includes `DatePickerDialog` shortcut).
  - Confirmation dialog for deletions.

State is stored with `rememberSaveable` (`showQuick`, `showEditor`, `cycleToEdit`, `cycleToDelete`), and Compose’s `AnimatedVisibility` reports validation errors inline.

---

## 9. BlueprintPeriodScreen (Detail)

Highlights:
- **LedgerHeader**: gradient capsule showing income, spent, animated progress bar, and “Adjust” button.
- **HighlightsCluster**: shows transaction count, largest spend, average spend.
- **Transactions list**: each entry in a rounded surface with independent edit/delete actions.
- **Modals**:
  - `IncomeAdjustDialog`: numeric validation with inline feedback.
  - `CycleMetaSheet`: allows label/month/year edits and integrates `DatePickerDialog`.
  - `TransactionSheet`: handles both add and edit with default today’s date.

The layout differs visually from other variants by using stacked capsules, progress bars, and earthy color accents.

---

## 10. BlueprintInsightScreen (Chart)

Implements a custom canvas chart:
- Plots income and spend lines with rounded markers.
- Fills the area under the income curve with a gradient for clarity.
- Draws soft horizontal grid lines and labels each month along the x-axis.
- Provides horizontally scrolling summary cards to review each cycle’s numbers.

Unlike other variants, this chart blends line + area presentation and uses dune colors to reinforce the brand.

---

## 11. Validation and error handling

* Quick dialogs disallow non-numeric income in real time.
* Month/year inputs clamp to sensible ranges (1–12, 2000–2100).
* Amounts must parse to doubles before they are saved.
* Repository calls operate inside try/catch internally (Room will throw) ensuring UI coroutines don’t crash; Compose surfaces remain stable.

---

## 12. Persistence and cascades

`PlanTransaction` includes `FOREIGN KEY(cycle_id) REFERENCES plan_cycles(id) ON DELETE CASCADE`, ensuring removing a cycle also removes its expenses. DAO `SUM(amount)` queries provide efficient totals used for the balance display and insight chart.

---

## 13. Surplus calculation and live updates

Balance is computed as `cycle.income - totalSpent` inside `CycleLedger`. Because both the cycle row and total aggregate are flows, any mutation (income updates, transaction edits) propagates automatically through `StateFlow` to the UI, guaranteeing immediate visual feedback.

---

## 14. User experience and accessibility

* Empty states prompt action (“Blueprint is blank”, “No expenses yet”).
* Date/time formatting is uniform via `SimpleDateFormat`.
* Touch targets follow Material 3 guidance; destructive buttons use distinct colors.
* Chart markers pair color with shape, aiding color-impaired readability.
* Bottom sheets provide clear cancel options to avoid accidental commits.

---

## 15. Testing and robustness

* Room generates SQL at compile time, catching query mistakes early.
* ViewModels keep coroutines off the main thread, removing UI jank.
* Manual smoke tests covered zero-income cycles, big amounts, invalid month/year entry, delete-with-expenses, and editing older months.
* The unidirectional data flow gives each screen a single source of truth, simplifying debugging.

---

## 16. Conclusion

Blueprint Budget fulfills the budgeting brief with a visually distinct, dune-inspired experience. Users can:
- Create and edit cycles with month/year defaults.
- Adjust income targets.
- Add, edit, and delete dated expenses.
- Track surplus instantly.
- Explore trends through a custom Compose chart with summary cards.

The layered architecture keeps the codebase maintainable, testable, and ready for future expansions like categories, widgets, or sync.



