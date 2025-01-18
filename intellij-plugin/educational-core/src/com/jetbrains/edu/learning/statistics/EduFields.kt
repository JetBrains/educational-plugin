package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.events.EventFields
import com.jetbrains.edu.learning.courseFormat.CourseMode

object EduFields {

  private const val TYPE = "type"
  private const val MODE = "mode"
  private const val LANGUAGE = "language"
  private const val PLATFORM = "platform"
  private const val COURSE_ID = "course_id"
  private const val MARKETPLACE_COURSE_VERSION = "marketplace_course_version"
  private const val TASK_ID = "task_id"

  private val PLATFORM_NAMES = listOf("PyCharm", "Coursera", "Hyperskill", "Marketplace")

  val COURSE_ID_FIELD = EventFields.Int(COURSE_ID)
  val TASK_ID_FIELD = EventFields.Int(TASK_ID)
  val COURSE_UPDATE_VERSION_FIELD = EventFields.Int(MARKETPLACE_COURSE_VERSION)
  val COURSE_MODE_FIELD = EventFields.Enum<CourseMode>(MODE)

  val ITEM_TYPE_FIELD = EventFields.String(TYPE, listOf(
    // course types
    *PLATFORM_NAMES.toTypedArray(),
    // section types
    "section",
    // lesson types
    "framework", "lesson",
    // task types
    "edu", "ide", "choice", "code", "output", "theory"
  ))

  val LANGUAGE_FIELD = EventFields.String(LANGUAGE, listOf(
    "JAVA", "kotlin", "Python", "Scala",
    "JavaScript", "Rust", "ObjectiveC", "go", "PHP"
  ))

  val PLATFORM_FIELD = EventFields.String(PLATFORM, PLATFORM_NAMES)
}
