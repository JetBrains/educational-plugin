package com.jetbrains.edu.learning

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.newproject.ui.myCourses.MyCoursesProvider.Companion.IS_FROM_MY_COURSES
import org.apache.commons.lang.LocaleUtils
import java.util.*


private const val HUMAN_LANGUAGE = "humanLanguage"
private const val PROGRAMMING_LANGUAGE = "programmingLanguage"

@State(name = "CoursesStorage", storages = [Storage("coursesStorage.xml", roamingType = RoamingType.DISABLED)])
@Service
class CoursesStorage : SimplePersistentStateComponent<UserCoursesState>(UserCoursesState()) {

  fun addCourse(course: Course, location: String, tasksSolved: Int = 0, tasksTotal: Int = 0) {
    state.addCourse(course, location, tasksSolved, tasksTotal)
  }

  fun getCoursePath(course: Course): String? = getCourseMetaInfo(course)?.location

  fun hasCourse(course: Course): Boolean = getCoursePath(course) != null

  fun getCourseMetaInfo(course: Course): CourseMetaInfo? {
    return state.courses.find { it.name == course.name && it.id == course.id && it.courseMode == course.courseMode }
  }

  fun updateCourseProgress(course: Course, location: String, tasksSolved: Int, tasksTotal: Int) {
    state.updateCourseProgress(course, location, tasksSolved, tasksTotal)
  }

  fun removeCourseByLocation(location: String) {
    val deletedCourse = state.removeCourseByLocation(location) ?: return
    ApplicationManager.getApplication().messageBus.syncPublisher(COURSE_DELETED).courseDeleted(deletedCourse)
  }

  companion object {
    val COURSE_DELETED = Topic.create("Edu.courseDeletedFromStorage", CourseDeletedListener::class.java)

    fun getInstance(): CoursesStorage = service()
  }
}

@Tag(EduNames.COURSE)
class CourseMetaInfo : Course {
  var location: String = ""
  var type: String = ""
  var tasksTotal: Int = 0
  var tasksSolved: Int = 0

  // to be compatible with previous version
  var programmingLanguageVersion: String? = null

  constructor() : super() {
    putUserData(IS_FROM_MY_COURSES, true)
  }

  constructor(location: String = "", course: Course, tasksTotal: Int = 0, tasksSolved: Int = 0) : this() {
    this.location = location
    this.type = course.itemType
    this.tasksTotal = tasksTotal
    this.tasksSolved = tasksSolved
    id = course.id
    name = course.name
    description = course.description
    courseMode = course.courseMode
    environment = course.environment
    language = course.language
    languageCode = course.languageCode
  }

  @Transient
  override fun getIndex(): Int {
    return super.getIndex()
  }

  @Transient
  override fun setIndex(index: Int) {
    super.setIndex(index)
  }

  @Transient
  override fun getItems(): List<StudyItem> {
    return super.getItems()
  }

  @Transient
  override fun setItems(items: List<StudyItem>) {
    super.setItems(items)
  }

  override fun getId(): Int {
    return myId
  }

  @OptionTag(HUMAN_LANGUAGE)
  override fun getLanguageCode(): String {
    return super.getLanguageCode()
  }

  private fun convertLanguageCode() {
    val languageCode = Locale.getAvailableLocales().find { it.displayName == languageCode }?.toLanguageTag()
    if (languageCode != null) {
      setLanguageCode(languageCode)
    }
    else {
      Logger.getInstance(this::class.java).warn("Cannot find locale for '${super.getLanguageCode()}'")
    }
  }

  override fun getHumanLanguage(): String {
    val locale = Locale.Builder().setLanguageTag(languageCode).build()
    if (languageCode.length > 3 && !LocaleUtils.isAvailableLocale(locale)) {
      convertLanguageCode()
    }
    return super.getHumanLanguage()
  }

  @OptionTag(HUMAN_LANGUAGE)
  override fun setLanguageCode(languageCode: String) {
    super.setLanguageCode(languageCode)
  }

  @OptionTag(PROGRAMMING_LANGUAGE)
  override fun getLanguage(): String {
    if (programmingLanguageVersion != null) {
      convertProgrammingLanguageVersion()
    }
    return super.getLanguage()
  }

  private fun convertProgrammingLanguageVersion() {
    language = "${super.getLanguage()} $programmingLanguageVersion"
    programmingLanguageVersion = null
  }

  override fun getLanguageVersion(): String? {
    if (programmingLanguageVersion != null) {
      convertProgrammingLanguageVersion()
    }

    return super.getLanguageVersion()
  }

  @OptionTag(PROGRAMMING_LANGUAGE)
  override fun setLanguage(language: String) {
    super.setLanguage(language)
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