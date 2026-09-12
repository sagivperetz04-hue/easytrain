package com.easytrain.feature.onboarding.chooserole

import com.easytrain.core.common.AppError
import com.easytrain.core.model.UserRole
import com.easytrain.core.testing.FakeProfileRepository
import com.easytrain.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ChooseRoleViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profileRepository = FakeProfileRepository()
    private val viewModel = ChooseRoleViewModel(profileRepository)

    @Test
    fun `choosing coach sets the role once`() =
        runTest {
            viewModel.onRoleChosen(UserRole.COACH)

            assertEquals(UserRole.COACH, profileRepository.roleSet)
            assertFalse(viewModel.state.value.isSubmitting)
        }

    @Test
    fun `a failure is shown and the role stays unset`() =
        runTest {
            profileRepository.failWith(AppError.Offline)

            viewModel.onRoleChosen(UserRole.TRAINEE)

            assertEquals(null, profileRepository.roleSet)
            assertEquals(AppError.Offline, viewModel.state.value.error)
        }
}
