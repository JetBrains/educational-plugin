package com.jetbrains.edu.learning.marketplace.license

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.marketplace.license.api.LicenseConnector
import com.jetbrains.edu.learning.marketplace.settings.LicenseLinkSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@Service(Service.Level.PROJECT)
class LicenseChecker(private val project: Project, scope: CoroutineScope) {
  private val checkRequests = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  private val _licenseState = MutableStateFlow<LicenseState?>(null)
  val licenseState: StateFlow<LicenseState?> = _licenseState.asStateFlow()

  val isRunning: Boolean
    @TestOnly
    get() = checkRequests.replayCache.isNotEmpty()

  val checkInterval: Long
    get() {
      // the default value is 18_000 seconds (5 hours)
      return TimeUnit.SECONDS.toMillis(Registry.intValue(REGISTRY_KEY).toLong())
    }

  init {
    scope.launch {
      checkRequests.debounce(300.milliseconds).collectLatest {
        doCheckLicense()
        delay(checkInterval)
        checkRequests.emit(Unit)
      }
    }
    scope.launch {
      licenseState.collectLatest {
        withContext(Dispatchers.EDT) {
          EditorNotifications.getInstance(project).updateAllNotifications()
        }
      }
    }
  }

  /**
   * If a checker has not been started, then it starts a periodic license check.
   *
   * If the checker is already running, it starts the check out of turn and restarts the timer.
   */
  fun scheduleLicenseCheck(): Boolean {
    if (!LicenseLinkSettings.isLicenseRequired(project)) return false
    return checkRequests.tryEmit(Unit)
  }

  private suspend fun doCheckLicense() {
    val link = LicenseLinkSettings.getInstance(project).link ?: return
    val licenseState = LicenseConnector.getInstance().checkLicense(link)
    _licenseState.update { licenseState }
  }

  companion object {
    const val REGISTRY_KEY: String = "edu.license.check.interval"

    fun getInstance(project: Project): LicenseChecker = project.service()
  }
}

enum class LicenseState {
  VALID, INVALID, ERROR
}