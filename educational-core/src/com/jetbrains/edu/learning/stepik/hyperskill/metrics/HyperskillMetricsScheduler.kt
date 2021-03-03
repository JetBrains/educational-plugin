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
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.handlers.HyperskillEventsHandler
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.handlers.HyperskillFrontendEventsHandler
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.handlers.HyperskillTimeSpentEventsHandler
import java.util.concurrent.TimeUnit

class HyperskillMetricsScheduler : AppLifecycleListener, DynamicPluginListener {

  override fun appFrameCreated(commandLineArgs: MutableList<String>) = startSendingEvents()

  override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) = startSendingEvents()

  private fun startSendingEvents() {
    if (isUnitTestMode) {
      return
    }

    scheduleEventsSendingJob { sendEvents() }
    scheduleEventsSendingJob { sendTimeSpentEvents() }
  }

  companion object {
    private const val HYPERSKILL_STATISTICS_INTERVAL_REGISTRY: String = "edu.hyperskill.metrics"

    @VisibleForTesting
    const val EVENTS_PER_REQUEST: Int = 1000

    private val LOG: Logger = logger<HyperskillMetricsScheduler>()

    private fun <Event> sendEvents(eventsHandler: HyperskillEventsHandler<Event>) {
      val eventsHandlerName = eventsHandler.javaClass.simpleName

      val pendingEvents = eventsHandler.pendingEvents
      if (pendingEvents.isEmpty()) {
        LOG.info("No data to send ($eventsHandlerName)")
        return
      }

      val remainingEvents = mutableListOf<Event>()

      for (eventsChunk in pendingEvents.chunked(EVENTS_PER_REQUEST)) {
        when (val res = eventsHandler.sendEvents(eventsChunk)) {
          is Ok -> {
            val sentEvents = res.value
            LOG.info("Successfully sent ${sentEvents.size} events ($eventsHandlerName)")
            if (LOG.isDebugEnabled) { // check debug level so as not to serialize events if not needed
              LOG.debug("Events (${eventsHandlerName})=${HyperskillConnector.getInstance().objectMapper.writeValueAsString(sentEvents)}")
            }
          }
          is Err -> {
            LOG.info("Failed to send events ($eventsHandlerName) with error `${res.error}`")
            remainingEvents.addAll(eventsChunk)
          }
        }
      }

      if (remainingEvents.isNotEmpty()) {
        eventsHandler.addPendingEvents(remainingEvents)
      }
    }

    private fun scheduleEventsSendingJob(command: Runnable) {
      val job = JobScheduler.getScheduler().scheduleWithFixedDelay(command, 0,
                                                                   Registry.intValue(HYPERSKILL_STATISTICS_INTERVAL_REGISTRY).toLong(),
                                                                   TimeUnit.MINUTES)
      Disposer.register(HyperskillMetricsService.getInstance(), Disposable { job.cancel(false) })
    }

    @VisibleForTesting
    fun sendEvents() = sendEvents(HyperskillFrontendEventsHandler)

    @VisibleForTesting
    fun sendTimeSpentEvents() = sendEvents(HyperskillTimeSpentEventsHandler)
  }
}