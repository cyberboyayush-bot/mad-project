package me.aysh.pocketledger.data.remote

import me.aysh.pocketledger.data.model.Category
import me.aysh.pocketledger.data.model.CategoryTotal
import me.aysh.pocketledger.data.model.DayTotal
import me.aysh.pocketledger.data.model.Expense
import me.aysh.pocketledger.data.model.ExpenseDraft
import me.aysh.pocketledger.data.model.MonthSummary
import me.aysh.pocketledger.data.model.Profile
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

internal fun ExpenseDto.toModel() = Expense(
    id = id,
    amountPaise = amount,
    category = Category.fromKey(category),
    note = note,
    spentOn = LocalDate.parse(spentOn),
    createdAt = Instant.parse(createdAt),
)

internal fun ExpenseDraft.toRequest() = ExpenseRequest(
    amount = amountPaise,
    category = category.key,
    note = note.trim(),
    spentOn = spentOn.toString(),
)

internal fun SummaryDto.toModel() = MonthSummary(
    month = YearMonth.parse(month),
    totalPaise = total,
    count = count,
    previousTotalPaise = previousTotal,
    budgetPaise = budget,
    byCategory = byCategory.map { CategoryTotal(Category.fromKey(it.category), it.total, it.count) },
    byDay = byDay.map { DayTotal(LocalDate.parse(it.date), it.total) },
)

internal fun ProfileDto.toModel() = Profile(
    id = id,
    name = name,
    email = email,
    monthlyBudgetPaise = monthlyBudget,
    memberSince = Instant.parse(createdAt),
)
