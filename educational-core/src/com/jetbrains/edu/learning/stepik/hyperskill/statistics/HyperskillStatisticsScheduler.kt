package com.jetbrains.edu.learning.stepik.hyperskill.statistics

import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillFrontendEvent
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import java.util.concurrent.TimeUnit

class HyperskillStatisticsScheduler : ProjectManagerListener {
  private val pendingEvents: MutableList<HyperskillFrontendEvent> = mutableListOf()

  override fun projectOpened(project: Project) {
    val course = project.course as? HyperskillCourse ?: return

    if (course.isStudy) {
      val future = JobScheduler.getScheduler().scheduleWithFixedDelay(
        {
          val newEvents = HyperskillStatisticsService.getInstance(project).allEvents()
          val events = pendingEvents + newEvents
          if (events.isEmpty()) {
            log("no data to send");
            return@scheduleWithFixedDelay
          }

          val sentEvents = HyperskillConnector.getInstance().sendEvents(events).onError {
            log("failed to send with error `$it`")
            pendingEvents.addAll(newEvents)
            return@scheduleWithFixedDelay
          }

          pendingEvents.clear()
          log("${sentEvents.events.size} events successfully sent")

        }, 0, Registry.intValue(HYPERSKILL_STATISTICS_INTERVAL_REGISTRY).toLong(), TimeUnit.MINUTES)

      Disposer.register(StudyTaskManager.getInstance(project), Disposable {
        future.cancel(false)
      })
    }
  }

  companion object {
    private const val HYPERSKILL_STATISTICS_INTERVAL_REGISTRY: String = "edu.hyperskill.statistics"

    private val LOG: Logger = logger<HyperskillStatisticsService>()

    private fun log(message: String) {
      LOG.info("Hyperskill statistics: $message")
    }
  }
}