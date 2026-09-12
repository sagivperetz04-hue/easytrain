package com.easytrain.core.common

/** Expected failures. Repositories return these instead of throwing. */
sealed interface AppError {
    data object Offline : AppError

    data object InvalidCredentials : AppError

    data object EmailAlreadyRegistered : AppError

    data object EmailNotConfirmed : AppError

    data object NotAuthenticated : AppError

    data class Validation(
        val message: String,
    ) : AppError

    data class Unexpected(
        val message: String?,
    ) : AppError
}
