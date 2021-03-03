package com.jetbrains.edu.learning.stepik.hyperskill.metrics

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillFrontendEvent
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillFrontendEventType
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.min

@State(name = "HyperskillMetrics", storages = [Storage("hyperskill.xml", roamingType = RoamingType.DISABLED)])
@Service
class HyperskillMetricsService : PersistentStateComponent<HyperskillMetricsService.State>, Disposable {
  private val events: Deque<HyperskillFrontendEvent> = ConcurrentLinkedDeque()

  fun viewEvent(task: Task?) {
    val hyperskillCourse = task?.course as? HyperskillCourse ?: return
    if (!hyperskillCourse.isStudy || isUnitTestMode) return

    doAddViewEvent(hyperskillCourse, task)
  }

  @VisibleForTesting
  fun doAddViewEvent(course: HyperskillCourse, task: Task) {
    val event = HyperskillFrontendEvent().apply {
      route = if (course.isTaskInProject(task)) stagePath(task) else stepPath(task)
      action = HyperskillFrontendEventType.VIEW
    }

    events.add(event)
  }

  fun addAll(pendingEvents: List<HyperskillFrontendEvent>) {
    pendingEvents.subList(0, min(EVENTS_LIMIT, pendingEvents.size)).asReversed().forEach { events.addFirst(it) }
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

  fun allEvents(emptyQueue: Boolean = true): List<HyperskillFrontendEvent> {
    val snapshot = mutableListOf<HyperskillFrontendEvent>()

    // non-blocking way to get snapshot of current events in queue
    val deathPill = HyperskillFrontendEvent()
    events.add(deathPill)

    val iterator = events.iterator()
    while (iterator.hasNext()) {
      val nextEvent = iterator.next()
      if (nextEvent === deathPill) {
        iterator.remove()
        break
      }
      if (emptyQueue) {
        iterator.remove()
      }
      snapshot.add(nextEvent)
    }

    return snapshot
  }

  override fun getState(): State {
    return State().apply {
      val pendingEvents = allEvents(false)
      events = pendingEvents.subList(0, min(pendingEvents.size, EVENTS_LIMIT)).toMutableList()
    }
  }

  override fun loadState(state: State) {
    events.addAll(state.events)
  }


  override fun dispose() {
    // do nothing
  }

  class State : BaseState() {
    @get:XCollection(style = XCollection.Style.v2)
    var events by list<HyperskillFrontendEvent>()
  }

  companion object {
    @JvmStatic
    fun getInstance(): HyperskillMetricsService = service()

    // it is approximately 300 bytes per event, lets keep hyperskill.xml file less than 2 MB
    @VisibleForTesting
    const val EVENTS_LIMIT: Int = 10000
  }
}