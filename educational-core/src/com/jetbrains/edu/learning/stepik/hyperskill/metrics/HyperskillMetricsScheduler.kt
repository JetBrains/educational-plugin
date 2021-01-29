package com.jetbrains.edu.learning.stepik.hyperskill.metrics

import com.google.common.annotations.VisibleForTesting
import com.intellij.concurrency.JobScheduler
import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillFrontendEvent
import java.util.concurrent.TimeUnit

class HyperskillMetricsScheduler : AppLifecycleListener, DynamicPluginListener {

  override fun appFrameCreated(commandLineArgs: MutableList<String>) = startSendingEvents()

  override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) = startSendingEvents()

  private fun startSendingEvents() {
    if (isUnitTestMode) {
      return
    }

    val future = JobScheduler.getScheduler().scheduleWithFixedDelay({ sendEvents() }, 0,
                                                                    Registry.intValue(HYPERSKILL_STATISTICS_INTERVAL_REGISTRY).toLong(),
                                                                    TimeUnit.MINUTES)

    Disposer.register(HyperskillMetricsService.getInstance(), Disposable { future.cancel(false) })
  }

  companion object {
    private const val HYPERSKILL_STATISTICS_INTERVAL_REGISTRY: String = "edu.hyperskill.metrics"

    @VisibleForTesting
    const val EVENTS_PER_REQUEST: Int = 1000

    private val LOG: Logger = logger<HyperskillMetricsScheduler>()

    @VisibleForTesting
    fun sendEvents() {
      val pendingEvents = HyperskillMetricsService.getInstance().allEvents()

      if (pendingEvents.isEmpty()) {
        LOG.info("No data to send")
        return
      }

      val remainingEvents = mutableListOf<HyperskillFrontendEvent>()

      for (eventsChunk in pendingEvents.chunked(EVENTS_PER_REQUEST)) {
        when (val res = HyperskillConnector.getInstance().sendEvents(eventsChunk)) {
          is Ok -> {
            val sentEvents = res.value.events
            LOG.info("Successfully sent ${sentEvents.size} events")
            if (LOG.isDebugEnabled) { // check debug level so as not to serialize events if not needed
              LOG.debug("Events=${HyperskillConnector.getInstance().objectMapper.writeValueAsString(sentEvents)}")
            }
          }
          is Err -> {
            LOG.info("Failed to send with error `${res.error}`")
            remainingEvents.addAll(eventsChunk)
          }
        }
      }

      if (remainingEvents.isNotEmpty()) {
        HyperskillMetricsService.getInstance().addAll(remainingEvents)
      }
    }
  }
}