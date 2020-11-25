package com.jetbrains.edu.learning.stepik.hyperskill.statistics

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillFrontendEvent
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillFrontendEventType
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@Service
class HyperskillStatisticsService {
  private val events: Queue<HyperskillFrontendEvent> = ConcurrentLinkedQueue()

  fun viewEvent(task: Task?) {
    val hyperskillCourse = task?.course as? HyperskillCourse ?: return
    val event = HyperskillFrontendEvent().apply {
      route = if (hyperskillCourse.isTaskInProject(task)) stagePath(task) else stepPath(task)
      action = HyperskillFrontendEventType.VIEW
    }

    events.add(event)
  }

  private fun stagePath(task: Task): String {
    val course = task.course as? HyperskillCourse ?: error("Course is not a Hyperskill course")
    val projectId = course.hyperskillProject?.id ?: error("Course doesn't have Hyperskill project")
    val stageId = course.stages[task.index - 1].id
    return "/projects/$projectId/stages/$stageId/implement"
  }

  private fun stepPath(task: Task): String {
    return "/learn/step/${task.id}"
  }

  fun allEvents(): List<HyperskillFrontendEvent> {
    val snapshot = mutableListOf<HyperskillFrontendEvent>()

    // non-blocking way to get snapshot of current events in queue
    val deathPill = HyperskillFrontendEvent()
    events.add(deathPill)

    val iterator = events.iterator()
    while (iterator.hasNext()) {
      val nextEvent = iterator.next()
      iterator.remove()
      if (nextEvent === deathPill) {
        break
      }
      snapshot.add(nextEvent)
    }

    return snapshot
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): HyperskillStatisticsService {
      return project.service()
    }
  }
}