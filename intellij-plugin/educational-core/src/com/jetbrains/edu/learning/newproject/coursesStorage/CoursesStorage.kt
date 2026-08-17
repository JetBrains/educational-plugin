package com.jetbrains.edu.learning.newproject.coursesStorage

import com.intellij.configurationStore.saveSettings
import com.intellij.ide.RecentProjectsManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CourseDataStorage
import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CourseInfo
import com.intellij.util.application
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import java.time.Instant
import java.util.UUID

@State(name = "CoursesStorage", storages = [Storage("coursesStorage.xml", roamingType = RoamingType.DISABLED)])
@Service
class CoursesStorage @JvmOverloads constructor(
  private val scope: CoroutineScope,
  private val newRecordId: () -> String = { UUID.randomUUID().toString() },
  private val now: () -> Instant = { Instant.now() },
) : SimplePersistentStateComponent<UserCoursesState>(UserCoursesState()),
    CourseDataStorage,
    EduTestAware {

  override fun loadState(state: UserCoursesState) {
    // We do not support Hyperskill and Stepik courses anymore.
    // But we still can have records with such types in the storage.
    // So, let's manually filter them out here not to expose them to other places.
    // Also, after the next state serialization, we won't have such records in storage anymore.
    state.courses.removeAll { it.type == EduFormatNames.HYPERSKILL || it.type == EduFormatNames.STEPIK }
    // Records created before `recordId` was introduced don't have it, so let's assign ids to them.
    // Dates are not backfilled since they are unknown for such records.
    state.courses.forEach {
      if (it.recordId == null) {
        it.recordId = newRecordId()
      }
    }
    super.loadState(state)
  }

  fun addCourse(course: Course, location: String, tasksSolved: Int = 0, tasksTotal: Int = 0) {
    val systemIndependentLocation = FileUtilRt.toSystemIndependentName(location)
    removeCourseRecordByLocation(systemIndependentLocation)
    state.courses.add(
      JBACourseFromStorage(
        location = systemIndependentLocation,
        course = course,
        recordId = newRecordId(),
        tasksTotal = tasksTotal,
        tasksSolved = tasksSolved,
        startedAt = now(),
      )
    )

    ApplicationManager.getApplication().messageBus.syncPublisher(COURSE_ADDED).courseAdded(course)
    saveState()
  }

  fun getCoursePath(course: Course): String? = getCourseMetaInfo(course)?.location

  override fun getCoursePath(courseInfo: CourseInfo): String? {
    return if (courseInfo is JBACourseFromStorage) {
      getCourseMetaInfo(courseInfo.name, courseInfo.id, courseInfo.courseMode, courseInfo.languageId)?.location
    }
    else {
      null
    }
  }

  fun hasCourse(course: Course): Boolean = getCoursePath(course) != null

  override fun removeCourseByLocation(location: String): Boolean {
    val deletedCourse = removeCourseRecordByLocation(location) ?: return false
    ApplicationManager.getApplication().messageBus.syncPublisher(COURSE_DELETED).courseDeleted(deletedCourse)
    RecentProjectsManager.getInstance().removePath(location)
    saveState()

    return true
  }

  /**
   * Removes the record for the given [location] and returns it, or returns `null` if there is no such record.
   */
  private fun removeCourseRecordByLocation(location: String): JBACourseFromStorage? {
    val record = state.courses.find { it.location == location } ?: return null
    state.courses.remove(record)
    return record
  }

  fun getCourseMetaInfo(course: Course): JBACourseFromStorage? =
    getCourseMetaInfo(course.name, course.id, course.courseMode, course.languageId)

  private fun getCourseMetaInfo(name: String, id: Int, courseMode: CourseMode, languageId: String): JBACourseFromStorage? {
    return state.courses.find {
      it.name == name
      && it.id == id
      && it.courseMode == courseMode
      && it.languageId == languageId
    }
  }

  fun updateCourseProgress(course: Course, location: String, tasksSolved: Int, tasksTotal: Int) {
    val systemIndependentLocation = FileUtilRt.toSystemIndependentName(location)
    val courseMetaInfo = state.courses.find { it.location == systemIndependentLocation }
    if (courseMetaInfo != null) {
      courseMetaInfo.tasksSolved = tasksSolved
      courseMetaInfo.tasksTotal = tasksTotal
      courseMetaInfo.lastUpdatedAt = now()
      state.intIncrementModificationCount()
    }
    else {
      state.courses.add(
        JBACourseFromStorage(
          location = systemIndependentLocation,
          course = course,
          recordId = newRecordId(),
          tasksTotal = tasksTotal,
          tasksSolved = tasksSolved,
          startedAt = now(),
        )
      )
    }
    saveState()
  }

  fun coursesInGroups(): List<CoursesGroup> {
    val courses = state.courses
    val solvedCourses = courses.filter { it.isStudy && it.tasksSolved != 0 && it.tasksSolved == it.tasksTotal }.map { it.toCourse() }
    val solvedCoursesGroup = CoursesGroup(EduCoreBundle.message("course.dialog.completed"), solvedCourses)

    val courseCreatorCoursesGroup = CoursesGroup(
      EduCoreBundle.message("course.dialog.my.courses.course.creation"),
      courses.filter { !it.isStudy }.map { it.toCourse() }
    )

    val inProgressCourses = courses.filter { it.isStudy && (it.tasksSolved == 0 || it.tasksSolved != it.tasksTotal) }.map { it.toCourse() }
    val inProgressCoursesGroup = CoursesGroup(EduCoreBundle.message("course.dialog.in.progress"), inProgressCourses)

    return listOf(courseCreatorCoursesGroup, inProgressCoursesGroup, solvedCoursesGroup).filter { it.courses.isNotEmpty() }
  }

  fun isNotEmpty(): Boolean = state.courses.isNotEmpty()

  override fun getAllCourses(): List<CourseInfo> {
    return state.courses
  }

  /**
   * Saves the state to the config file as early as possible.
   *
   * Used to dump state to the config file as early as possible
   * since Toolbox integration tracks the state of this service via the corresponding config file on filesystem
   */
  private fun saveState() {
    // Saving data in unit tests leads to unexpected errors in different places
    if (isUnitTestMode) return

    scope.launch {
      // Unfortunately, public API allows saving only all components instead of a single one.
      // In theory, it can lead to some performance issues because of too frequent saving settings of all components.
      // In practice, it's not expected since storage's state modification happens not so often
      saveSettings(application, forceSavingAllSettings = true)
    }
  }

  @TestOnly
  override fun cleanUpState() {
    state.courses.clear()
  }

  companion object {
    val COURSE_DELETED = Topic.create("Edu.courseDeletedFromStorage", CourseDeletedListener::class.java)
    val COURSE_ADDED = Topic.create("Edu.courseAddedToStorage", CourseAddedListener::class.java)

    fun getInstance(): CoursesStorage = service()
  }
}

class UserCoursesState : BaseState() {
  //  courses list is not updated on course removal and could contain removed courses.
  @get:XCollection(style = XCollection.Style.v2)
  val courses by list<JBACourseFromStorage>()
}
