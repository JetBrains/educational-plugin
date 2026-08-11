package com.jetbrains.edu.learning.newproject.ui.welcomeScreen

import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CourseInfo
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.CourseraCourse
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSERA
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.newproject.ui.logo
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.swing.Icon

private const val PROGRAMMING_LANGUAGE = "programmingLanguage"
private const val PROGRAMMING_LANGUAGE_ID = "programmingLanguageId"
private const val PROGRAMMING_LANGUAGE_VERSION = "programmingLanguageVersion"

@Tag(EduNames.COURSE)
class JBACourseFromStorage() : CourseInfo() {
  var type: String = ""
  var courseMode: CourseMode = CourseMode.STUDENT
  var isMarketplace: Boolean = true
  var environment: String = ""
  val itemType: String
    @Transient
    get() = type

  private var programmingLanguage: String = ""
    @OptionTag(PROGRAMMING_LANGUAGE)
    get() {
      if (programmingLanguageVersion != null) {
        field = "$field $programmingLanguageVersion"
        programmingLanguageVersion = null
      }
      return field
    }

  @OptionTag(PROGRAMMING_LANGUAGE_VERSION)
  var languageVersion: String? = null
    get() {
      if (programmingLanguageVersion != null) {
        programmingLanguage = "$programmingLanguage $programmingLanguageVersion"
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

  /**
   * Unique identifier of the record in the storage.
   *
   * Unlike [id], it's unique for each record since the same course may be located in several places at the same time.
   * It's necessary for external integrations (for example, Toolbox) to be able to refer to a particular record.
   *
   * It's `null` only for records created before this property was introduced. Such records get an id on state loading.
   */
  var recordId: String? = null

  /**
   * When the corresponding course project was started, i.e. when this record was created.
   */
  @OptionTag(converter = InstantConverter::class)
  var startedAt: Instant? = null

  /**
   * When the progress of the corresponding course project was updated last time.
   */
  @OptionTag(converter = InstantConverter::class)
  var lastUpdatedAt: Instant? = null

  constructor(
    location: String = "",
    course: Course,
    recordId: String,
    tasksTotal: Int = 0,
    tasksSolved: Int = 0,
    startedAt: Instant? = null,
    lastUpdatedAt: Instant? = startedAt
  ) : this() {
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
    this.recordId = recordId
    this.tasksTotal = tasksTotal
    this.tasksSolved = tasksSolved
    this.startedAt = startedAt
    this.lastUpdatedAt = lastUpdatedAt
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
      COURSERA -> CourseraCourse()
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

/**
 * Stores [Instant] as an ISO-8601 string.
 */
private class InstantConverter : Converter<Instant>() {
  override fun toString(value: Instant): String = value.toString()

  override fun fromString(value: String): Instant? = try {
    Instant.parse(value)
  }
  catch (_: DateTimeParseException) {
    null
  }
}
