package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.TestOnly

/**
 * Keeps track of which study items should be visible in the Course View.
 *
 * While [isSubtreeModeEnabled] is `false`, all study items of the course are considered visible.
 * When it is `true`, only the items revealed with [markStudyItemAsVisible] are visible,
 * so a learner is shown a subtree of the course instead of the whole course.
 *
 * The set of revealed items is stored per project and survives project reopening.
 *
 * See [CourseViewPane]
 */
@Service(Service.Level.PROJECT)
@State(name = "CourseViewVisibleItems", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class CourseViewVisibleItems(private val project: Project, private val scope: CoroutineScope) :
  SerializablePersistentStateComponent<CourseViewVisibleItems.State>(State()), EduTestAware {

  /**
   * Determines if only the study items marked with [markStudyItemAsVisible] should be visible in the current project.
   */
  var isSubtreeModeEnabled: Boolean
    get() = state.subtreeModeEnabled
    set(value) {
      if (state.subtreeModeEnabled == value) return
      updateState {
        if (!value) {
          State() // reset state
        }
        else {
          it.copy(subtreeModeEnabled = true)
        }

      }
      refreshCourseView()
    }

  /**
   * Ids of the tasks revealed so far.
   */
  @get:TestOnly
  val visibleTaskIds: Set<Int>
    get() = state.visibleTaskIds

  /**
   * Marks [item] and all its child items visible to a learner in the Course View
   */
  fun markStudyItemAsVisible(item: StudyItem) {
    if (!isSubtreeModeEnabled) return

    val newIds = item.childTaskIds() - state.visibleTaskIds
    if (newIds.isEmpty()) return

    updateState { it.copy(visibleTaskIds = it.visibleTaskIds + newIds) }
    refreshCourseView()
  }

  /**
   * Determines if [item] should be shown to a learner in the Course View.
   *
   * A lesson or a section is visible if at least one of its tasks is visible.
   * Tasks of a [FrameworkLesson] are visible all together with the lesson itself
   * because such a lesson is always shown as a single item.
   *
   * Returns `true` for any item if [isSubtreeModeEnabled] is `false`, if the course is not being studied,
   * or if the item has no id (it happens for courses without study item ids, for example, local ones).
   */
  fun shouldBeShown(item: StudyItem): Boolean {
    if (!isSubtreeModeEnabled) return true
    if (!item.course.isStudy) return true

    return when (item) {
      is Task -> {
        val lesson = item.lesson
        if (lesson is FrameworkLesson) shouldBeShown(lesson) else item.shouldBeShown
      }
      is Lesson -> item.taskList.any { it.shouldBeShown }
      is Section -> item.lessons.any { shouldBeShown(it) }
      else -> true
    }
  }

  @TestOnly
  override fun cleanUpState() {
    updateState { State() }
  }

  private val Task.shouldBeShown: Boolean
    get() = id == 0 || id in state.visibleTaskIds

  private fun StudyItem.childTaskIds(): Set<Int> {
    val tasks = when (this) {
      is Course -> allTasks
      is Section -> lessons.flatMap { it.taskList }
      is Lesson -> taskList
      is Task -> listOf(this)
      else -> emptyList()
    }
    return tasks.mapNotNullTo(hashSetOf()) { it.id.takeIf { id -> id != 0 } }
  }

  private fun refreshCourseView() {
    scope.launch(Dispatchers.EDT) {
      // ProjectView.refresh() updates only the current pane.
      // So, if the current pane is not Course View, it won't be updated.
      ProjectView.getInstance(project).getProjectViewPaneById(CourseViewPane.ID)?.updateFromRoot(true)
    }
  }

  companion object {
    fun getInstance(project: Project): CourseViewVisibleItems = project.service()
  }

  @Serializable
  data class State(
    val subtreeModeEnabled: Boolean = false,
    val visibleTaskIds: Set<Int> = emptySet(),
  )
}
