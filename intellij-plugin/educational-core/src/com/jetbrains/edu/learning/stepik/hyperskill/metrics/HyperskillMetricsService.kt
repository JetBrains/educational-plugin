package com.jetbrains.edu.learning.stepik.hyperskill.metrics

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.projectView.CourseViewUtils.isSolved
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillFrontendEvent
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillFrontendEventType
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTimeSpentEvent
import org.jetbrains.annotations.TestOnly
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.DoubleAdder
import kotlin.math.min

@State(name = "HyperskillMetrics", storages = [Storage("hyperskill.xml", roamingType = RoamingType.DISABLED)])
open class HyperskillMetricsService : PersistentStateComponent<HyperskillMetricsService.State>, Disposable, EduTestAware {
  private val frontendEvents: Deque<HyperskillFrontendEvent> = ConcurrentLinkedDeque()
  private val timeSpentEvents: MutableMap<Int, DoubleAdder> = mutableMapOf()
  private var taskInProgress: Pair<Int, Long>? = null

  private val lock = Object()

  fun viewEvent(task: Task?) {
    val hyperskillCourse = task?.course as? HyperskillCourse ?: return
    if (!hyperskillCourse.isStudy) return

    doAddViewEvent(task)
    taskStarted(task)
  }

  fun taskStarted(task: Task) {
    if (task.isSolved) {
      return
    }
    val id = task.id
    taskStarted(id)
  }

  @VisibleForTesting
  fun taskStarted(id: Int) {
    synchronized(lock) {

      // Stop tracking of previous task
      taskStopped()

      // There is no need to track task with corrupted 0 id
      if (id != 0) {
        taskInProgress = id to System.currentTimeMillis()
      }
    }
  }

  fun taskStopped() {
    synchronized(lock) {
      val (id, start) = taskInProgress ?: return
      val duration = toDuration(start)
      timeSpentEvents.computeIfAbsent(id) { DoubleAdder() }.add(duration)

      taskInProgress = null
    }
  }

  protected open fun toDuration(start: Long): Double = (System.currentTimeMillis() - start).toDouble() / 1000

  @VisibleForTesting
  fun doAddViewEvent(task: Task) {
    if (task.id == 0) return

    val event = HyperskillFrontendEvent().apply {
      route = task.getRoute()
      action = HyperskillFrontendEventType.VIEW
    }

    frontendEvents.add(event)
  }

  fun addAllFrontendEvents(pendingEvents: List<HyperskillFrontendEvent>) {
    pendingEvents.subList(0, min(FRONTEND_EVENTS_LIMIT, pendingEvents.size)).asReversed().forEach { frontendEvents.addFirst(it) }
  }

  fun allFrontendEvents(emptyQueue: Boolean = true): List<HyperskillFrontendEvent> {
    val snapshot = mutableListOf<HyperskillFrontendEvent>()

    // non-blocking way to get snapshot of current events in queue
    val deathPill = HyperskillFrontendEvent()
    frontendEvents.add(deathPill)

    val iterator = frontendEvents.iterator()
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

  fun allTimeSpentEvents(reset: Boolean): List<HyperskillTimeSpentEvent> {
    return pendingTimeSpentEvents(reset).map {
      HyperskillTimeSpentEvent().apply {
        step = it.key
        duration = it.value
      }
    }
  }

  fun addAllTimeSpentEvents(pendingTimeSpentEvents: Map<Int, Double>) {
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
    val pendingFrontendEvents = allFrontendEvents(false)

    return State().apply {
      events = pendingFrontendEvents.subList(0, min(pendingFrontendEvents.size, FRONTEND_EVENTS_LIMIT)).toMutableList()
      timeSpentEvents = pendingTimeSpentEvents.toMutableMap()
    }
  }

  override fun loadState(state: State) {
    frontendEvents.addAll(state.events)
    addAllTimeSpentEvents(state.timeSpentEvents)
  }

  override fun dispose() {
    // do nothing
  }

  @TestOnly
  override fun cleanUpState() {
    frontendEvents.clear()
    timeSpentEvents.clear()
    taskInProgress = null
  }

  class State : BaseState() {
    @get:XCollection(style = XCollection.Style.v2)
    var events by list<HyperskillFrontendEvent>()

    @get:XCollection(style = XCollection.Style.v2)
    var timeSpentEvents by map<Int, Double>()
  }

  companion object {
    fun getInstance(): HyperskillMetricsService = service()

    // it is approximately 300 bytes per event, lets keep hyperskill.xml file less than 2 MB (except time spent events)
    // time spent events are limited by the number of steps
    @VisibleForTesting
    const val FRONTEND_EVENTS_LIMIT: Int = 10000

    @VisibleForTesting
    fun Task.getRoute(): String {
      val course = course as? HyperskillCourse ?: error("Course is not a Hyperskill course")
      return if (course.isTaskInProject(this)) {
        val projectId = course.hyperskillProject?.id ?: error("Course doesn't have Hyperskill project")
        val stageId = course.stages[index - 1].id
        "/projects/$projectId/stages/$stageId/implement"
      }
      else {
        "/learn/step/$id"
      }
    }
  }
}