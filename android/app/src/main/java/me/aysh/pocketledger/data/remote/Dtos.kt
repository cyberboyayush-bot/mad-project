package me.aysh.pocketledger.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequest(val name: String, val email: String, val password: String)

@Serializable
data class SignInRequest(val email: String, val password: String)

@Serializable
data class ExpenseDto(
    val id: String,
    val amount: Long,
    val category: String,
    val note: String = "",
    val spentOn: String,
    val createdAt: String,
)

@Serializable
data class ExpenseRequest(val amount: Long, val category: String, val note: String, val spentOn: String)

@Serializable
data class ExpenseResponse(val expense: ExpenseDto)

@Serializable
data class ExpensesResponse(val expenses: List<ExpenseDto>)

@Serializable
data class CategoryTotalDto(val category: String, val total: Long, val count: Int)

@Serializable
data class DayTotalDto(val date: String, val total: Long)

@Serializable
data class SummaryDto(
    val month: String,
    val total: Long,
    val count: Int,
    val previousTotal: Long,
    val budget: Long,
    val byCategory: List<CategoryTotalDto>,
    val byDay: List<DayTotalDto>,
)

@Serializable
data class ProfileDto(
    val id: String,
    val name: String,
    val email: String,
    val monthlyBudget: Long,
    val createdAt: String,
)

@Serializable
data class ProfileResponse(val profile: ProfileDto)

/** Null fields are omitted from the JSON body, so only the provided fields are updated. */
@Serializable
data class UpdateProfileRequest(val name: String? = null, val monthlyBudget: Long? = null)

/** Covers both the app API shape `{ error: { code, message } }` and Better Auth's `{ code, message }`. */
@Serializable
data class ErrorBody(
    val error: ErrorDetail? = null,
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class ErrorDetail(val code: String? = null, val message: String? = null)

@Serializable
class EmptyBody
