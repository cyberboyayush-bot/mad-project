package me.aysh.pocketledger.util

object Validation {

    const val MIN_PASSWORD = 8
    const val MAX_PASSWORD = 128
    const val MAX_NAME = 60
    const val MAX_NOTE = 120

    private val EMAIL = Regex("""^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$""")

    fun name(value: String): String? = when {
        value.isBlank() -> "Enter your name"
        value.trim().length > MAX_NAME -> "Name is too long"
        else -> null
    }

    fun email(value: String): String? = when {
        value.isBlank() -> "Enter your email"
        !EMAIL.matches(value.trim()) -> "Enter a valid email address"
        else -> null
    }

    /** Sign-in only checks presence so we don't leak password rules for existing accounts. */
    fun passwordPresent(value: String): String? = if (value.isEmpty()) "Enter your password" else null

    fun newPassword(value: String): String? = when {
        value.isEmpty() -> "Choose a password"
        value.length < MIN_PASSWORD -> "Use at least $MIN_PASSWORD characters"
        value.length > MAX_PASSWORD -> "Password is too long"
        else -> null
    }

    /** 0–4 score used for the password strength meter. */
    fun passwordStrength(value: String): Int {
        if (value.length < MIN_PASSWORD) return if (value.isEmpty()) 0 else 1
        var score = 1
        if (value.length >= 12) score++
        if (value.any(Char::isDigit) && value.any(Char::isLetter)) score++
        if (value.any { !it.isLetterOrDigit() } || (value.any(Char::isUpperCase) && value.any(Char::isLowerCase))) score++
        return score.coerceAtMost(4)
    }

    fun amount(input: String): String? = when {
        input.isBlank() -> "Enter an amount"
        Money.parseToPaise(input) == null -> "Enter an amount between ₹0.01 and ₹10,00,000"
        else -> null
    }
}
