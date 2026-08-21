package com.example.couchpilot.settings.presentation

import com.example.couchpilot.core.data.PreferencesRepository
import com.example.couchpilot.core.domain.LocalDataManager
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private val localDataManager: LocalDataManager = mockk(relaxed = true)
    private val preferencesRepository: PreferencesRepository = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = SettingsViewModel(localDataManager, preferencesRepository)
    }

    @Test
    fun `clearLocalData clears everything and reports didClear`() {
        viewModel.clearLocalData()

        coVerify { localDataManager.clearAllLocalData() }
        assertTrue(viewModel.uiState.value.didClear)
        assertFalse(viewModel.uiState.value.isClearing)
    }

    @Test
    fun `retakeOnboarding resets the onboarding flag without touching local data`() {
        viewModel.retakeOnboarding()

        coVerify { preferencesRepository.setOnboardingCompleted(false) }
        coVerify(exactly = 0) { localDataManager.clearAllLocalData() }
    }
}
