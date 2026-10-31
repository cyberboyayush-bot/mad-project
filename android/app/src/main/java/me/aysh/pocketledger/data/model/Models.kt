package me.aysh.pocketledger.data.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

enum class Category(val key: String, val label: String) {
    Food("food", "Food"),
    Transport("transport", "Transport"),
    Shopping("shopping", "Shopping"),
    Bills("bills", "Bills"),
    Entertainment("entertainment", "Fun"),
    Health("health", "Health"),
    Education("education", "Education"),
    Other("other", "Other");

    companion object {
        fun fromKey(key: String): Category = entries.firstOrNull { it.key == key } ?: Other
    }
}

/** All money values are stored in paise (1 rupee = 100 paise) to avoid floating point errors. */
data class Expense(
    val id: String,
    val amountPaise: Long,
    val category: Category,
    val note: String,
    val spentOn: LocalDate,
    val createdAt: Instant,
)

data class ExpenseDraft(
    val amountPaise: Long,
    val category: Category,
    val note: String,
    val spentOn: LocalDate,
)

data class CategoryTotal(val category: Category, val totalPaise: Long, val count: Int)

data class DayTotal(val date: LocalDate, val totalPaise: Long)

data class MonthSummary(
    val month: YearMonth,
    val totalPaise: Long,
    val count: Int,
    val previousTotalPaise: Long,
    val budgetPaise: Long,
    val byCategory: List<CategoryTotal>,
    val byDay: List<DayTotal>,
) {
    val hasBudget: Boolean get() = budgetPaise > 0
    val remainingPaise: Long get() = budgetPaise - totalPaise
    val budgetProgress: Float
        get() = if (budgetPaise <= 0) 0f else (totalPaise.toFloat() / budgetPaise).coerceAtLeast(0f)
}

data class Profile(
    val id: String,
    val name: String,
    val email: String,
    val monthlyBudgetPaise: Long,
    val memberSince: Instant,
)
