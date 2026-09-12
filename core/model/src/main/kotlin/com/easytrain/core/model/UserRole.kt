package com.easytrain.core.model

/**
 * Chosen once at onboarding and immutable afterwards — switching sides means a second account.
 */
enum class UserRole(
    val wireValue: String,
) {
    COACH("coach"),
    TRAINEE("trainee"),
    ;

    companion object {
        fun fromWireValue(value: String?): UserRole? = entries.firstOrNull { it.wireValue == value }
    }
}
