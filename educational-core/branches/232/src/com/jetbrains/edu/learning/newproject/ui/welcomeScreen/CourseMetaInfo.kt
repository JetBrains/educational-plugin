package com.jetbrains.edu.learning.newproject.ui.welcomeScreen

import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CourseInfo
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.newproject.ui.logo
import javax.swing.Icon

private const val PROGRAMMING_LANGUAGE = "programmingLanguage"

// BACKCOMPACT: 2023.1
@Tag(EduNames.COURSE)
class CourseMetaInfo() : CourseInfo() {
  var type: String = ""
  var courseMode = CourseMode.STUDENT
  var environment = ""
  val itemType
    @Transient
    get() = type

  private var programmingLanguage: String = ""
    @OptionTag(PROGRAMMING_LANGUAGE)
    get() {
      if (programmingLanguageVersion != null) {
        field = "${field} $programmingLanguageVersion"
        programmingLanguageVersion = null
      }
      return field
    }

  var languageVersion: String? = null
    get() {
      if (programmingLanguageVersion != null) {
        programmingLanguage = "${programmingLanguage} $programmingLanguageVersion"
        programmingLanguageVersion = null
      }

      return field
    }

  var languageId: String = ""

  // to be compatible with previous version
  private var programmingLanguageVersion: String? = null

  override var icon: Icon?
    get() = this.toCourse().logo
    set(value) {}

  constructor(location: String = "", course: Course, tasksTotal: Int = 0, tasksSolved: Int = 0) : this() {
    this.type = course.itemType
    id = course.id
    name = course.name
    description = course.description
    courseMode = course.courseMode
    environment = course.environment
    languageId = course.languageId
    languageVersion = course.languageVersion
    this.location = location
    this.tasksTotal = tasksTotal
    this.tasksSolved = tasksSolved
  }

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

  val isStudy = courseMode == CourseMode.STUDENT

  fun toCourse(): EduCourse {
    val eduCourse = EduCourse()
    eduCourse.id = id
    eduCourse.name = name
    eduCourse.description = description
    eduCourse.courseMode = courseMode
    eduCourse.environment = environment
    eduCourse.languageId = languageId
    eduCourse.languageVersion = languageVersion
    return eduCourse
  }

  private fun convertProgrammingLanguageVersion(value: String) {
    value.split(" ").apply {
      languageId = first()
      languageVersion = getOrNull(1)
    }
  }
}
