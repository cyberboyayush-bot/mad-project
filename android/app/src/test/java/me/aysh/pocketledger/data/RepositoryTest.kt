package me.aysh.pocketledger.data

import kotlinx.coroutines.test.runTest
import me.aysh.pocketledger.data.model.Category
import me.aysh.pocketledger.data.model.ExpenseDraft
import me.aysh.pocketledger.data.remote.AppException
import me.aysh.pocketledger.data.remote.AppJson
import me.aysh.pocketledger.data.remote.createApi
import me.aysh.pocketledger.data.repository.AuthRepository
import me.aysh.pocketledger.data.repository.LedgerRepository
import me.aysh.pocketledger.data.repository.TokenStore
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class RepositoryTest {

    private lateinit var server: MockWebServer
    private var token: String? = null
    private var unauthorizedCalls = 0

    private val tokenStore = object : TokenStore {
        override suspend fun save(token: String) { this@RepositoryTest.token = token }
        override suspend fun clear() { token = null }
    }

    private lateinit var auth: AuthRepository
    private lateinit var ledger: LedgerRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = createApi(
            baseUrl = server.url("/").toString(),
            tokenProvider = { token },
            onUnauthorized = { unauthorizedCalls++ },
        )
        auth = AuthRepository(api, tokenStore, AppJson)
        ledger = LedgerRepository(api, AppJson)
    }

    @After
    fun tearDown() = server.close()

    private fun json(code: Int, body: String, headers: Map<String, String> = emptyMap()) {
        val builder = MockResponse.Builder().code(code).body(body).addHeader("Content-Type", "application/json")
        headers.forEach { (k, v) -> builder.addHeader(k, v) }
        server.enqueue(builder.build())
    }

    private suspend fun expectFailure(block: suspend () -> Unit): AppException {
        try {
            block()
        } catch (e: AppException) {
            return e
        }
        fail("Expected AppException")
        error("unreachable")
    }

    @Test
    fun `sign in stores the token from the response header`() = runTest {
        json(200, """{"user":{"id":"1","name":"A","email":"a@b.co"},"token":"x"}""", mapOf("set-auth-token" to "abc.sig"))
        auth.signIn("  A@B.co ", "password1")
        assertEquals("abc.sig", token)
        val request = server.takeRequest()
        assertEquals("/api/auth/sign-in/email", request.url.encodedPath)
        assertTrue(request.body!!.utf8().contains("\"email\":\"a@b.co\""))
    }

    @Test
    fun `wrong credentials map to a friendly message and do not clear the session`() = runTest {
        json(401, """{"code":"INVALID_EMAIL_OR_PASSWORD","message":"Invalid email or password"}""")
        val error = expectFailure { auth.signIn("a@b.co", "nope") }
        assertEquals("Incorrect email or password", error.message)
        assertNull(token)
        assertEquals(0, unauthorizedCalls)
    }

    @Test
    fun `duplicate email on sign up`() = runTest {
        json(422, """{"code":"USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL","message":"User already exists"}""")
        val error = expectFailure { auth.signUp("A", "a@b.co", "password1") }
        assertEquals("An account with this email already exists", error.message)
    }

    @Test
    fun `missing token header is an error`() = runTest {
        json(200, """{"user":{"id":"1","name":"A","email":"a@b.co"}}""")
        val error = expectFailure { auth.signIn("a@b.co", "password1") }
        assertTrue(error.message.contains("session"))
        assertNull(token)
    }

    @Test
    fun `sign out clears the token even if the server fails`() = runTest {
        token = "abc.sig"
        json(500, """{}""")
        auth.signOut()
        assertNull(token)
    }

    @Test
    fun `requests carry the bearer token and 401 triggers sign out`() = runTest {
        token = "abc.sig"
        json(401, """{"error":{"code":"unauthorized","message":"Please sign in"}}""")
        val error = expectFailure { ledger.profile() }
        assertEquals(401, error.status)
        assertEquals("Bearer abc.sig", server.takeRequest().headers["Authorization"])
        assertEquals(1, unauthorizedCalls)
    }

    @Test
    fun `server validation messages are passed through`() = runTest {
        token = "t.s"
        json(400, """{"error":{"code":"bad_request","message":"amount: Too big"}}""")
        val error = expectFailure {
            ledger.addExpense(ExpenseDraft(1, Category.Food, "", LocalDate.of(2026, 10, 1)))
        }
        assertEquals("amount: Too big", error.message)
    }

    @Test
    fun `unreachable server gives a connection message`() = runTest {
        server.close()
        val error = expectFailure { ledger.profile() }
        assertTrue(error.message.contains("reach the server"))
    }

    @Test
    fun `parses expenses and sends filters`() = runTest {
        token = "t.s"
        json(
            200,
            """{"expenses":[{"id":"e1","amount":12550,"category":"transport","note":"Auto","spentOn":"2026-10-05","createdAt":"2026-10-05T10:00:00.000Z","extra":1}]}""",
        )
        val result = ledger.expenses(month = YearMonth.of(2026, 10), category = Category.Transport, query = "  auto ")
        assertEquals(1, result.size)
        with(result.first()) {
            assertEquals(12_550L, amountPaise)
            assertEquals(Category.Transport, category)
            assertEquals(LocalDate.of(2026, 10, 5), spentOn)
        }
        val url = server.takeRequest().url
        assertEquals("2026-10", url.queryParameter("month"))
        assertEquals("transport", url.queryParameter("category"))
        assertEquals("auto", url.queryParameter("q"))
    }

    @Test
    fun `unknown categories fall back to Other`() = runTest {
        token = "t.s"
        json(200, """{"expense":{"id":"e1","amount":1,"category":"crypto","spentOn":"2026-10-05","createdAt":"2026-10-05T10:00:00Z"}}""")
        assertEquals(Category.Other, ledger.expense("e1").category)
    }

    @Test
    fun `writes bump the revision so screens reload`() = runTest {
        token = "t.s"
        val before = ledger.revision.value
        json(201, """{"expense":{"id":"e1","amount":100,"category":"food","note":"","spentOn":"2026-10-05","createdAt":"2026-10-05T10:00:00Z"}}""")
        ledger.addExpense(ExpenseDraft(100, Category.Food, " tea ", LocalDate.of(2026, 10, 5)))
        assertEquals(before + 1, ledger.revision.value)
        assertTrue(server.takeRequest().body!!.utf8().contains("\"note\":\"tea\""))

        server.enqueue(MockResponse.Builder().code(204).build())
        ledger.deleteExpense("e1")
        assertEquals(before + 2, ledger.revision.value)
    }

    @Test
    fun `profile update only sends provided fields`() = runTest {
        token = "t.s"
        json(200, """{"profile":{"id":"u","name":"A","email":"a@b.co","monthlyBudget":500000,"createdAt":"2026-10-01T00:00:00Z"}}""")
        val profile = ledger.updateProfile(monthlyBudgetPaise = 500_000)
        assertEquals(500_000L, profile.monthlyBudgetPaise)
        assertEquals("""{"monthlyBudget":500000}""", server.takeRequest().body!!.utf8())
    }

    @Test
    fun `summary maps to the model`() = runTest {
        token = "t.s"
        json(
            200,
            """{"month":"2026-10","total":80000,"count":3,"previousTotal":99900,"budget":500000,
               "byCategory":[{"category":"food","total":30000,"count":2}],
               "byDay":[{"date":"2026-10-05","total":70000}]}""",
        )
        val summary = ledger.summary(YearMonth.of(2026, 10))
        assertEquals(420_000L, summary.remainingPaise)
        assertEquals(0.16f, summary.budgetProgress, 0.0001f)
        assertEquals(Category.Food, summary.byCategory.single().category)
    }
}
