package com.jetbrains.edu.learning.newproject.coursesStorage

import com.intellij.ide.RecentProjectsManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import org.apache.commons.lang.LocaleUtils
import java.util.*


private const val HUMAN_LANGUAGE = "humanLanguage"
private const val PROGRAMMING_LANGUAGE = "programmingLanguage"
private const val PROGRAMMING_LANGUAGE_ID = "programmingLanguageId"
private const val PROGRAMMING_LANGUAGE_VERSION = "programmingLanguageVersion"

@State(name = "CoursesStorage", storages = [Storage("coursesStorage.xml", roamingType = RoamingType.DISABLED)])
@Service
class CoursesStorage : SimplePersistentStateComponent<UserCoursesState>(UserCoursesState()) {

  fun addCourse(course: Course, location: String, tasksSolved: Int = 0, tasksTotal: Int = 0) {
    state.addCourse(course, location, tasksSolved, tasksTotal)
    ApplicationManager.getApplication().messageBus.syncPublisher(COURSE_ADDED).courseAdded(course)
  }

  fun getCoursePath(course: Course): String? = getCourseMetaInfo(course)?.location

  fun hasCourse(course: Course): Boolean = getCoursePath(course) != null

  fun getCourseMetaInfoForAnyLanguage(course: Course): CourseMetaInfo? {
    return state.courses.find {
      it.name == course.name
      && it.id == course.id
      && it.courseMode == course.courseMode
    }
  }

  fun getCourseMetaInfo(course: Course): CourseMetaInfo? {
    return state.courses.find {
      it.name == course.name
      && it.id == course.id
      && it.courseMode == course.courseMode
      && it.languageId == course.languageId
    }
  }

  fun updateCourseProgress(course: Course, location: String, tasksSolved: Int, tasksTotal: Int) {
    state.updateCourseProgress(course, location, tasksSolved, tasksTotal)
  }

  fun removeCourseByLocation(location: String) {
    val deletedCourse = state.removeCourseByLocation(location) ?: return
    ApplicationManager.getApplication().messageBus.syncPublisher(COURSE_DELETED).courseDeleted(deletedCourse)
    RecentProjectsManager.getInstance().removePath(location)
  }

  fun coursesInGroups(): List<CoursesGroup> {
    val courses = state.courses.toMutableList()
    val solvedCourses = CoursesGroup(EduCoreBundle.message("course.dialog.completed"),
                                     courses.filter { it.isStudy && it.tasksSolved != 0 && it.tasksSolved == it.tasksTotal })
    val courseCreatorCourses = CoursesGroup(EduCoreBundle.message("course.dialog.my.courses.course.creation"),
                                            courses.filter { !it.isStudy })
    val inProgressCourses = CoursesGroup(EduCoreBundle.message("course.dialog.in.progress"),
                                         courses.filter { it.isStudy && (it.tasksSolved == 0 || it.tasksSolved != it.tasksTotal) })

    return listOf(courseCreatorCourses, inProgressCourses, solvedCourses).filter { it.courses.isNotEmpty() }
  }

  fun isNotEmpty() = state.courses.isNotEmpty()

  companion object {
    val COURSE_DELETED = Topic.create("Edu.courseDeletedFromStorage", CourseDeletedListener::class.java)
    val COURSE_ADDED = Topic.create("Edu.courseAddedToStorage", CourseAddedListener::class.java)

    fun getInstance(): CoursesStorage = service()
  }
}

@Tag(EduNames.COURSE)
class CourseMetaInfo() : Course() {
  var type: String = ""
  var location: String = ""
  var tasksTotal: Int = 0
  var tasksSolved: Int = 0

  override var parent: ItemContainer
    @Transient
    get() = super.parent
    @Transient
    set(_) {}

  constructor(location: String = "", course: Course, tasksTotal: Int = 0, tasksSolved: Int = 0) : this() {
    this.type = course.itemType
    id = course.id
    name = course.name
    description = course.description
    courseMode = course.courseMode
    environment = course.environment
    languageId = course.languageId
    languageVersion = course.languageVersion
    languageCode = course.languageCode
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

  override val humanLanguage: String
    get() {
      try {
        val locale = Locale.Builder().setLanguageTag(languageCode).build()
        if (languageCode.length > 3 && !LocaleUtils.isAvailableLocale(locale)) {
          convertLanguageCode()
        }
      }
      catch (e: IllformedLocaleException) {
        convertLanguageCode()
      }
      return super.humanLanguage
    }

  @OptionTag(HUMAN_LANGUAGE)
  override var languageCode = super.languageCode

  private fun convertLanguageCode() {
    val languageCode = Locale.getAvailableLocales().find { it.displayName == this.languageCode }?.toLanguageTag()
    if (languageCode != null) {
      this.languageCode = languageCode
    }
    else {
      Logger.getInstance(this::class.java).warn("Cannot find locale for '${super.languageCode}'")
    }
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