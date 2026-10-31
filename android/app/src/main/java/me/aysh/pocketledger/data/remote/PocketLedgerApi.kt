package me.aysh.pocketledger.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PocketLedgerApi {

    @POST("api/auth/sign-up/email")
    suspend fun signUp(@Body body: SignUpRequest): Response<AuthResponse>

    @POST("api/auth/sign-in/email")
    suspend fun signIn(@Body body: SignInRequest): Response<AuthResponse>

    @POST("api/auth/sign-out")
    suspend fun signOut(@Body body: EmptyBody = EmptyBody()): Response<Unit>

    @GET("api/me")
    suspend fun profile(): ProfileResponse

    @PATCH("api/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): ProfileResponse

    @GET("api/expenses")
    suspend fun expenses(
        @Query("month") month: String? = null,
        @Query("category") category: String? = null,
        @Query("q") query: String? = null,
    ): ExpensesResponse

    @GET("api/expenses/{id}")
    suspend fun expense(@Path("id") id: String): ExpenseResponse

    @POST("api/expenses")
    suspend fun createExpense(@Body body: ExpenseRequest): ExpenseResponse

    @PATCH("api/expenses/{id}")
    suspend fun updateExpense(@Path("id") id: String, @Body body: ExpenseRequest): ExpenseResponse

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: String): Response<Unit>

    @GET("api/summary")
    suspend fun summary(@Query("month") month: String): SummaryDto
}
