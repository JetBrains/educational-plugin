package com.jetbrains.edu.remote.termination

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.rdserver.unattendedHost.UnattendedStatusUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NonNls
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit.SECONDS
import kotlin.time.toDuration

@Service(Service.Level.APP)
class EduRemoteInactivityWatcherService(private val scope: CoroutineScope) {
  private val isStarted = AtomicBoolean(false)
  private val checkInterval = 1.minutes
  private val maxIdleTime: Long
    //default value is 3600 seconds (60 minutes, 1 hour)
    get() = System.getProperty(DELAY_SYSTEM_PROPERTY_NAME)?.toLongOrNull() ?: TimeUnit.HOURS.toSeconds(1)

  @Suppress("UnstableApiUsage", "DuplicatedCode")
  fun start() {
    if (!isStarted.compareAndSet(false, true)) {
      LOG.info("There was an attempt to start the service again, while it is already running")
      return
    }
    scope.launch {
      while (true) {
        val secondsSinceLastActivity = UnattendedStatusUtil.getStatus().projects?.firstOrNull()?.secondsSinceLastControllerActivity
        if (secondsSinceLastActivity == null) {
          delay(checkInterval)
          continue
        }
        val remainingTime = maxIdleTime - secondsSinceLastActivity
        if (remainingTime <= 0) {
          LOG.info("Exiting application due to long user inactivity for $secondsSinceLastActivity seconds")
          ApplicationManager.getApplication().exit(true, true, false)
          break
        }
        LOG.debug(
          "User is inactive for $secondsSinceLastActivity seconds, " +
          "in $remainingTime seconds the IDE will be shut down if the user doesn't return"
        )
        delay(remainingTime.toDuration(SECONDS))
      }
    }
  }

  companion object {
    @NonNls
    private const val DELAY_SYSTEM_PROPERTY_NAME = "edu.remote.ide.inactivity.termination.delay.seconds"
    private val LOG = logger<EduRemoteInactivityWatcherService>()
  }
}
