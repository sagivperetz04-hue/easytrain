package com.easytrain.feature.onboarding.profilesetup

import com.easytrain.core.model.Units
import com.easytrain.core.model.UserRole
import com.easytrain.core.testing.FakeProfileRepository
import com.easytrain.core.testing.MainDispatcherRule
import com.easytrain.core.testing.testProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileSetupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profileRepository = FakeProfileRepository()

    @Test
    fun `the form starts from whatever the profile already has`() =
        runTest {
            profileRepository.emit(
                testProfile(role = UserRole.TRAINEE, displayName = "Ari", units = Units.LB),
            )

            val viewModel = ProfileSetupViewModel(profileRepository)

            assertEquals("Ari", viewModel.state.value.displayName)
            assertEquals(Units.LB, viewModel.state.value.units)
        }

    @Test
    fun `saving sends the name, units and the device timezone`() =
        runTest {
            val viewModel = ProfileSetupViewModel(profileRepository)
            var doneCalled = false

            viewModel.onDisplayNameChanged("Dana")
            viewModel.onUnitsChanged(Units.LB)
            viewModel.onSubmit { doneCalled = true }

            val saved = profileRepository.savedProfile
            assertEquals("Dana", saved?.first)
            assertEquals(Units.LB, saved?.second)
            assertTrue(saved?.third?.isNotBlank() == true)
            assertTrue(doneCalled)
        }

    @Test
    fun `a blank name cannot be submitted`() =
        runTest {
            val viewModel = ProfileSetupViewModel(profileRepository)

            viewModel.onSubmit {}

            assertEquals(null, profileRepository.savedProfile)
        }
}
