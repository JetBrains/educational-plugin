package com.jetbrains.edu.learning.newproject.ui.welcomeScreen

import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CourseInfo
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.newproject.ui.logo
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_TYPE
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import javax.swing.Icon

private const val PROGRAMMING_LANGUAGE = "programmingLanguage"
private const val PROGRAMMING_LANGUAGE_ID = "programmingLanguageId"
private const val PROGRAMMING_LANGUAGE_VERSION = "programmingLanguageVersion"

// BACKCOMPACT: 2023.1
@Tag(EduNames.COURSE)
class CourseMetaInfo() : CourseInfo() {
  var type: String = ""
  var courseMode = CourseMode.STUDENT
  var isMarketplace: Boolean = true
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

  @OptionTag(PROGRAMMING_LANGUAGE_VERSION)
  var languageVersion: String? = null
    get() {
      if (programmingLanguageVersion != null) {
        programmingLanguage = "${programmingLanguage} $programmingLanguageVersion"
        programmingLanguageVersion = null
      }

      return field
    }

  @OptionTag(PROGRAMMING_LANGUAGE_ID)
  var languageId: String = ""

  // to be compatible with previous version
  private var programmingLanguageVersion: String? = null

  override var icon: Icon?
    @Transient
    get() = this.toCourse().logo
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
    isMarketplace = course.isMarketplace
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

  val isStudy: Boolean
    get() = this.courseMode == CourseMode.STUDENT

  fun toCourse(): Course {
    val eduCourse = when (itemType) {
      CourseraNames.COURSE_TYPE -> CourseraCourse()
      CheckiONames.CHECKIO_TYPE -> CheckiOCourse()
      HYPERSKILL_TYPE -> HyperskillCourse()
      StepikNames.STEPIK_TYPE -> StepikCourse()
      CodeforcesNames.CODEFORCES_COURSE_TYPE -> CodeforcesCourse()
      EduNames.EDU, MARKETPLACE -> EduCourse()
      else -> EduCourse()
    }

    eduCourse.id = id
    eduCourse.name = name
    eduCourse.description = description
    eduCourse.courseMode = courseMode
    eduCourse.environment = environment
    eduCourse.languageId = languageId
    eduCourse.languageVersion = languageVersion
    eduCourse.isMarketplace = isMarketplace
    return eduCourse
  }

  private fun convertProgrammingLanguageVersion(value: String) {
    value.split(" ").apply {
      languageId = first()
      languageVersion = getOrNull(1)
    }
  }
}
