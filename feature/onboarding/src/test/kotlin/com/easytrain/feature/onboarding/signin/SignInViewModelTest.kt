package com.easytrain.feature.onboarding.signin

import app.cash.turbine.test
import com.easytrain.core.common.AppError
import com.easytrain.core.testing.FakeAuthRepository
import com.easytrain.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val viewModel = SignInViewModel(authRepository)

    @Test
    fun `submitting is blocked until both fields are filled`() {
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.onEmailChanged("dana@example.com")
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.onPasswordChanged("secret123")
        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `a successful sign-in passes the trimmed credentials to the repository`() =
        runTest {
            viewModel.onEmailChanged("dana@example.com")
            viewModel.onPasswordChanged("secret123")

            viewModel.onSubmit()

            assertEquals("dana@example.com" to "secret123", authRepository.signedInWith)
            assertFalse(viewModel.state.value.isSubmitting)
            assertEquals(null, viewModel.state.value.error)
        }

    @Test
    fun `a rejected sign-in surfaces the error and stops submitting`() =
        runTest {
            authRepository.failWith(AppError.InvalidCredentials)
            viewModel.onEmailChanged("dana@example.com")
            viewModel.onPasswordChanged("wrong")

            viewModel.state.test {
                assertEquals(null, awaitItem().error)

                viewModel.onSubmit()

                assertTrue(awaitItem().isSubmitting)

                val failed = awaitItem()
                assertEquals(AppError.InvalidCredentials, failed.error)
                assertFalse(failed.isSubmitting)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `typing again clears the previous error`() {
        authRepository.failWith(AppError.InvalidCredentials)
        viewModel.onEmailChanged("dana@example.com")
        viewModel.onPasswordChanged("wrong")
        viewModel.onSubmit()

        viewModel.onPasswordChanged("secret123")

        assertEquals(null, viewModel.state.value.error)
    }
}
