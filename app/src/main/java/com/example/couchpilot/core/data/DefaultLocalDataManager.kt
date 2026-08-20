package com.example.couchpilot.core.data

import com.example.couchpilot.core.database.CouchPilotDatabase
import com.example.couchpilot.core.domain.LocalDataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The only class that talks to both the Room database and the DataStore-backed onboarding
 * flag - named as a manager rather than a repository since it doesn't expose reads, only the
 * single "wipe everything" coordination action.
 */
class DefaultLocalDataManager @Inject constructor(
    private val database: CouchPilotDatabase,
    private val preferencesRepository: PreferencesRepository
) : LocalDataManager {
    override suspend fun clearAllLocalData() {
        // clearAllTables() is a blocking Room call - Room asserts this itself and crashes
        // (IllegalStateException) if it's ever reached from the main thread.
        withContext(Dispatchers.IO) {
            database.clearAllTables()
        }
        preferencesRepository.setOnboardingCompleted(false)
    }
}
