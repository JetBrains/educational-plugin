package com.jetbrains.edu.learning.stepik.hyperskill.metrics

import com.intellij.concurrency.JobScheduler
import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.handlers.HyperskillEventsHandler
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.handlers.HyperskillFrontendEventsHandler
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.handlers.HyperskillTimeSpentEventsHandler
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.TimeUnit

class HyperskillMetricsScheduler : AppLifecycleListener, DynamicPluginListener {

  override fun appFrameCreated(commandLineArgs: MutableList<String>) = startSendingEvents()

  override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) = startSendingEvents()

  private fun startSendingEvents() {
    if (isUnitTestMode) {
      return
    }

    scheduleEventsSendingJob { sendFrontendEvents() }
    scheduleEventsSendingJob { sendTimeSpentEvents() }
  }

  companion object {
    private const val HYPERSKILL_STATISTICS_INTERVAL_REGISTRY: String = "edu.hyperskill.metrics"

    @VisibleForTesting
    const val EVENTS_PER_REQUEST: Int = 1000

    private val LOG: Logger = logger<HyperskillMetricsScheduler>()

    private fun <Event> sendEvents(eventsHandler: HyperskillEventsHandler<Event>) {
      val eventsHandlerName = eventsHandler.javaClass.simpleName

      if (HyperskillSettings.INSTANCE.account == null) {
        LOG.info("Hyperskill user logged out. No events were sent.")
        return
      }

      val pendingEvents = eventsHandler.pendingEvents
      if (pendingEvents.isEmpty()) {
        LOG.info("No data to send ($eventsHandlerName)")
        return
      }

      val remainingEvents = mutableListOf<Event>()

      for (eventsChunk in pendingEvents.chunked(EVENTS_PER_REQUEST)) {
        if (HyperskillSettings.INSTANCE.account == null) {
          remainingEvents.addAll(eventsChunk)
          continue
        }
        when (val res = eventsHandler.sendEvents(eventsChunk)) {
          is Ok -> {
            LOG.info("Successfully sent ${eventsChunk.size} events ($eventsHandlerName)")
            if (LOG.isDebugEnabled) { // check debug level so as not to serialize events if not needed
              LOG.debug("Events (${eventsHandlerName})=${eventsChunk.map { it }}")
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

    private fun scheduleEventsSendingJob(sendEvents: Runnable) {
      val job = JobScheduler.getScheduler().scheduleWithFixedDelay(sendEvents, 0,
                                                                   Registry.intValue(HYPERSKILL_STATISTICS_INTERVAL_REGISTRY).toLong(),
                                                                   TimeUnit.MINUTES)
      Disposer.register(HyperskillMetricsService.getInstance()) { job.cancel(false) }
    }

    @VisibleForTesting
    fun sendFrontendEvents() = sendEvents(HyperskillFrontendEventsHandler)

    @VisibleForTesting
    fun sendTimeSpentEvents() = sendEvents(HyperskillTimeSpentEventsHandler)
  }
}