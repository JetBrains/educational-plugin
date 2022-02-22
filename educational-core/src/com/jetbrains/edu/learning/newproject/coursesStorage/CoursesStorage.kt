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
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import org.apache.commons.lang.LocaleUtils
import java.util.*


private const val HUMAN_LANGUAGE = "humanLanguage"
private const val PROGRAMMING_LANGUAGE = "programmingLanguage"

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
      && it.languageID == course.languageID
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

  // to be compatible with previous version
  var programmingLanguageVersion: String? = null

  constructor(location: String = "", course: Course, tasksTotal: Int = 0, tasksSolved: Int = 0) : this() {
    this.type = course.itemType
    id = course.id
    name = course.name
    description = course.description
    courseMode = course.courseMode
    environment = course.environment
    language = course.language
    languageCode = course.languageCode
    this.location = location
    this.tasksTotal = tasksTotal
    this.tasksSolved = tasksSolved
  }

  @Transient
  override fun getItemType(): String {
    return type
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

  override var language: String
    @OptionTag(PROGRAMMING_LANGUAGE)
    get() {
      if (programmingLanguageVersion != null) {
        convertProgrammingLanguageVersion()
      }
      return super.language
    }
    @OptionTag(PROGRAMMING_LANGUAGE)
    set(value) {
      super.language = value
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

  private fun convertProgrammingLanguageVersion() {
    language = "${super.language} $programmingLanguageVersion"
    programmingLanguageVersion = null
  }

  override val languageVersion: String?
    get() {
      if (programmingLanguageVersion != null) {
        convertProgrammingLanguageVersion()
      }

      return super.languageVersion
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