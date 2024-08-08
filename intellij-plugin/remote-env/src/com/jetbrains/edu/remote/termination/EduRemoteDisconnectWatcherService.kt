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
import kotlin.time.Duration.Companion.seconds

@Service(Service.Level.APP)
class EduRemoteDisconnectWatcherService(private val scope: CoroutineScope) {
  private val isStarted = AtomicBoolean(false)
  private val checkInterval = 5.seconds
  private val maxIdleTime: Long
    //default value is 600 seconds (10 minutes)
    get() = System.getProperty(DELAY_SYSTEM_PROPERTY_NAME)?.toLongOrNull() ?: TimeUnit.MINUTES.toSeconds(10)

  @Volatile
  private var timeDisconnectionHappened: Long = 0

  @Suppress("UnstableApiUsage", "DuplicatedCode")
  fun start() {
    if (!isStarted.compareAndSet(false, true)) {
      error("There was an attempt to start the service again, while it is already running")
    }
    scope.launch {
      while (true) {
        val isControllerConnected = UnattendedStatusUtil.getStatus().projects?.firstOrNull()?.controllerConnected
        when {
          // First check after disconnection
          isControllerConnected == false && timeDisconnectionHappened == 0L -> {
            LOG.debug("User disconnected")
            timeDisconnectionHappened = System.currentTimeMillis()
          }

          isControllerConnected == true && timeDisconnectionHappened != 0L -> {
            LOG.debug("Reconnection happened")
            timeDisconnectionHappened = 0L
          }

          isControllerConnected == false && timeDisconnectionHappened != 0L -> {
            val timeDisconnected = (System.currentTimeMillis() - timeDisconnectionHappened) / 1000
            LOG.debug("Disconnection continues for $timeDisconnected seconds, max idle time: $maxIdleTime")
            if (timeDisconnected >= maxIdleTime) {
              LOG.info("Exiting application due to long user absence")
              ApplicationManager.getApplication().exit(false, true, false)
              break
            }
          }
        }
        delay(checkInterval)
      }
    }
  }

  companion object {
    @NonNls
    private const val DELAY_SYSTEM_PROPERTY_NAME = "edu.remote.ide.disconnect.termination.delay.seconds"
    private val LOG = logger<EduRemoteDisconnectWatcherService>()
  }
}