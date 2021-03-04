package com.jetbrains.edu.learning.stepik.hyperskill.metrics

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillFrontendEvent
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillFrontendEventType
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTimeSpentEvent
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.DoubleAdder
import kotlin.math.min

@State(name = "HyperskillMetrics", storages = [Storage("hyperskill.xml", roamingType = RoamingType.DISABLED)])
open class HyperskillMetricsService : PersistentStateComponent<HyperskillMetricsService.State>, Disposable {
  private val events: Deque<HyperskillFrontendEvent> = ConcurrentLinkedDeque()

  private val timeSpentEvents: MutableMap<Int, DoubleAdder> = mutableMapOf()
  private var taskInProgress: Pair<Int, Long>? = null

  private val lock = Object()

  fun viewEvent(task: Task?) {
    val hyperskillCourse = task?.course as? HyperskillCourse ?: return
    if (!hyperskillCourse.isStudy || isUnitTestMode) return

    doAddViewEvent(hyperskillCourse, task)
    taskStarted(task.id)
  }

  fun taskStarted(id: Int) {
    synchronized(lock) {
      taskStopped()
      taskInProgress = id to System.currentTimeMillis()
    }
  }

  fun taskStopped() {
    synchronized(lock) {
      val curTask = taskInProgress ?: return

      val (id, start) = curTask
      val duration = toDuration(start)
      timeSpentEvents.computeIfAbsent(id) { DoubleAdder() }.add(duration)

      taskInProgress = null
    }
  }

  protected open fun toDuration(start: Long): Double = (System.currentTimeMillis() - start).toDouble() / 1000

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

  @VisibleForTesting
  fun allTimeSpentEvents(reset: Boolean): List<HyperskillTimeSpentEvent> {
    return pendingTimeSpentEvents(reset).map {
      HyperskillTimeSpentEvent().apply {
        step = it.key
        duration = it.value
      }
    }
  }

  fun addAll(pendingTimeSpentEvents: Map<Int, Double>) {
    for (event in pendingTimeSpentEvents) {
      val id = event.key
      val duration = event.value
      timeSpentEvents.computeIfAbsent(id) { DoubleAdder() }.add(duration)
    }
  }

  private fun pendingTimeSpentEvents(reset: Boolean = true): Map<Int, Double> {
    return timeSpentEvents.mapValues {
      val value = it.value
      if (reset) value.sumThenReset() else value.sum()
    }.filterValues { it != 0.0 }
  }

  override fun getState(): State {
    val pendingTimeSpentEvents = pendingTimeSpentEvents(reset = false)
    val pendingEvents = allEvents(false)

    return State().apply {
      events = pendingEvents.subList(0, min(pendingEvents.size, EVENTS_LIMIT)).toMutableList()
      timeSpentEvents = pendingTimeSpentEvents.toMutableMap()
    }
  }

  override fun loadState(state: State) {
    events.addAll(state.events)
    addAll(state.timeSpentEvents)
  }

  override fun dispose() {
    // do nothing
  }

  class State : BaseState() {
    @get:XCollection(style = XCollection.Style.v2)
    var events by list<HyperskillFrontendEvent>()

    @get:XCollection(style = XCollection.Style.v2)
    var timeSpentEvents by map<Int, Double>()
  }

  companion object {
    @JvmStatic
    fun getInstance(): HyperskillMetricsService = service()

    // it is approximately 300 bytes per event, lets keep hyperskill.xml file less than 2 MB (except time spent events)
    // time spent events are limited by the number of steps
    @VisibleForTesting
    const val EVENTS_LIMIT: Int = 10000
  }
}