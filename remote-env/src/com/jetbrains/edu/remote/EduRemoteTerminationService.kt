package com.jetbrains.edu.remote

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.rdserver.unattendedHost.UnattendedStatusUtil
import kotlinx.coroutines.*
import org.jetbrains.annotations.NonNls
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Service(Service.Level.APP)
class EduRemoteTerminationService(private val scope: CoroutineScope) {
  private val checkInterval: Duration = 5.seconds
  private val maxIdleTime: Int
    //default value is 600 seconds (10 minutes)
    get() = System.getProperty(DELAY_SYSTEM_PROPERTY_NAME)?.toIntOrNull() ?: 600

  @Volatile
  private var timeDisconnectionHappened: Long = 0

  @Suppress("UnstableApiUsage")
  fun start() {
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
              ApplicationManager.getApplication().exit(true, true, false)
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
    private const val DELAY_SYSTEM_PROPERTY_NAME = "edu.remote.ide.termination.delay.seconds"
    private val LOG = logger<EduRemoteTerminationService>()
  }
}