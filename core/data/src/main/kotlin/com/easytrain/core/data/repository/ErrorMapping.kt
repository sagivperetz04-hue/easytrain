package com.easytrain.core.data.repository

import com.easytrain.core.common.AppError
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import java.io.IOException

/**
 * Supabase reports expected outcomes (bad password, duplicate email) as exceptions; the rest of the
 * app deals in [AppError] instead.
 */
internal fun Throwable.asAppError(): AppError =
    when (this) {
        is IOException, is HttpRequestException -> AppError.Offline
        is AuthRestException ->
            when (errorCode?.value) {
                "invalid_credentials" -> AppError.InvalidCredentials
                "user_already_exists", "email_exists" -> AppError.EmailAlreadyRegistered
                "email_not_confirmed" -> AppError.EmailNotConfirmed
                else -> AppError.Unexpected(message)
            }
        else -> AppError.Unexpected(message)
    }
