package com.jetbrains.edu.learning.newproject.coursesStorage

import com.intellij.ide.RecentProjectsManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CourseInfo
import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CoursesStorage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduLanguage
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup


private const val PROGRAMMING_LANGUAGE = "programmingLanguage"
private const val PROGRAMMING_LANGUAGE_ID = "programmingLanguageId"
private const val PROGRAMMING_LANGUAGE_VERSION = "programmingLanguageVersion"

@State(name = "CoursesStorage", storages = [Storage("coursesStorage.xml", roamingType = RoamingType.DISABLED)])
@Service
class JBCoursesStorage : SimplePersistentStateComponent<UserCoursesState>(UserCoursesState()), CoursesStorage {


  fun addCourse(course: Course, location: String, tasksSolved: Int = 0, tasksTotal: Int = 0) {
    state.addCourse(course, location, tasksSolved, tasksTotal)
    ApplicationManager.getApplication().messageBus.syncPublisher(COURSE_ADDED).courseAdded(course)
  }

  fun getCoursePath(course: Course): String? = getCoursePath(CourseMetaInfo().apply {
    this.name = course.name
    this.id = course.id
    this.courseMode = course.courseMode
    this.programmingLanguage = course.programmingLanguage
  })

  fun hasCourse(course: Course): Boolean = getCoursePath(course) != null

  fun getCourseMetaInfoForAnyLanguage(course: Course): CourseMetaInfo? {
    return state.courses.find {
      it.name == course.name
      && it.id == course.id
      && it.courseMode == course.courseMode
    }
  }

  fun getCourseMetaInfo(name: String, id: Int, courseMode: CourseMode, languageId: String): CourseMetaInfo? {
    return state.courses.find {
      it.name == course.name
      && it.id == course.id
      && it.courseMode == course.courseMode
      && it.languageID == course.languageId
    }
  }

  fun updateCourseProgress(course: Course, location: String, tasksSolved: Int, tasksTotal: Int) {
    state.updateCourseProgress(course, location, tasksSolved, tasksTotal)
  }

  override fun getCoursePath(courseInfo: CourseInfo): String? {
    return if (courseInfo is CourseMetaInfo) {
      getCourseMetaInfo(courseInfo.name, courseInfo.id, courseInfo.courseMode, courseInfo.languageID)?.location
    }
    else {
      null
    }
  }

  override fun removeCourseByLocation(location: String): Boolean {
    val deletedCourse = state.removeCourseByLocation(location) ?: return false
    ApplicationManager.getApplication().messageBus.syncPublisher(COURSE_DELETED).courseDeleted(deletedCourse)
    RecentProjectsManager.getInstance().removePath(location)

    return true
  }

  override fun getAllCourses(): List<CourseInfo> {
    return state.courses.toMutableList()
  }

  fun coursesInGroups(): List<CoursesGroup> {
    val courses = state.courses.toMutableList()
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
  fun isNotEmpty() = state.courses.isNotEmpty()

  companion object {
    val COURSE_DELETED = Topic.create("Edu.courseDeletedFromStorage", CourseDeletedListener::class.java)
    val COURSE_ADDED = Topic.create("Edu.courseAddedToStorage", CourseAddedListener::class.java)

    fun getInstance(): JBCoursesStorage = service()
  }
}

@Tag(EduNames.COURSE)
class CourseMetaInfo() : CourseInfo() {
  var type: String = ""
  var courseMode = CourseMode.STUDENT
  var environment = ""
  val itemType
    @Transient
    get() = type

  var programmingLanguage: String = ""
    @OptionTag(PROGRAMMING_LANGUAGE)
    get() {
      if (programmingLanguageVersion != null) {
        field = "${field} $programmingLanguageVersion"
        programmingLanguageVersion = null
      }
      return field
    }
    @OptionTag(PROGRAMMING_LANGUAGE)
    set

  var languageVersion: String? = null
    get() {
      if (programmingLanguageVersion != null) {
        programmingLanguage = "${programmingLanguage} $programmingLanguageVersion"
        programmingLanguageVersion = null
      }

      return field
    }

  val languageID: String
    get() = EduLanguage.get(programmingLanguage).id

  // to be compatible with previous version
  var programmingLanguageVersion: String? = null

  constructor(location: String = "", course: Course, tasksTotal: Int = 0, tasksSolved: Int = 0) : this() {
    this.type = course.itemType
    id = course.id
    name = course.name
    description = course.description
    courseMode = course.courseMode
    environment = course.environment
    languageId = course.languageId
    languageVersion = course.languageVersion
    languageVersion = course.languageVersion
    this.location = location
    this.tasksTotal = tasksTotal
    this.tasksSolved = tasksSolved
  }

  override val itemType
    @Transient
    get() = type

  /**
   * Used only for migration, see EDU-5856
   */
  @Suppress("MemberVisibilityCanBePrivate", "unused")
  var oldProgrammingLanguage: String? = null
    @OptionTag(PROGRAMMING_LANGUAGE)
    set(value) {
      if (value == null) return
      convertProgrammingLanguageVersion(value)
      field = null
    }

  override var languageId: String
    @OptionTag(PROGRAMMING_LANGUAGE_ID)
    get() = super.languageId
    @OptionTag(PROGRAMMING_LANGUAGE_ID)
    set(value) {
      if (value.isEmpty()) return
      super.languageId = value
    }

  override var languageVersion: String?
    @OptionTag(PROGRAMMING_LANGUAGE_VERSION)
    get() = super.languageVersion
    @OptionTag(PROGRAMMING_LANGUAGE_VERSION)
    set(value) {
      if (value == null) return
      super.languageVersion = value
    }
  val isStudy = courseMode == CourseMode.STUDENT

  fun toCourse(): EduCourse {
    val eduCourse = EduCourse()
    eduCourse.id = id
    eduCourse.name = name
    eduCourse.description = description
    eduCourse.courseMode = courseMode
    eduCourse.environment = environment
    eduCourse.programmingLanguage = programmingLanguage
    return eduCourse
  }

  private fun convertProgrammingLanguageVersion(value: String) {
    value.split(" ").apply {
      super.languageId = first()
      super.languageVersion = getOrNull(1)
    }
  }
}

class UserCoursesState : BaseState() {
  //  courses list is not updated on course removal and could contain removed courses.
  @get:XCollection(style = XCollection.Style.v2)
  val courses by list<CourseMetaInfo>()

  fun addCourse(course: Course, location: String, tasksSolved: Int = 0, tasksTotal: Int = 0) {
    val systemIndependentLocation = FileUtilRt.toSystemIndependentName(location)
    courses.removeIf { it.location == systemIndependentLocation }
    val courseMetaInfo = CourseMetaInfo(systemIndependentLocation, course, tasksTotal, tasksSolved)
    courses.add(courseMetaInfo)
  }

  fun removeCourseByLocation(location: String): CourseMetaInfo? {
    val courseMetaInfo = courses.find { it.location == location }
    courses.remove(courseMetaInfo)
    return courseMetaInfo
  }

  fun updateCourseProgress(course: Course, location: String, tasksSolved: Int, tasksTotal: Int) {
    val systemIndependentLocation = FileUtilRt.toSystemIndependentName(location)
    val courseMetaInfo = courses.find { it.location == systemIndependentLocation }
    if (courseMetaInfo != null) {
      courseMetaInfo.tasksSolved = tasksSolved
      courseMetaInfo.tasksTotal = tasksTotal
      intIncrementModificationCount()
    }
    else {
      courses.add(CourseMetaInfo(systemIndependentLocation, course, tasksTotal, tasksSolved))
    }
  }
}