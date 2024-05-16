package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.events.EventFields
import com.jetbrains.edu.learning.courseFormat.CourseMode

object EduFields {

  private const val TYPE = "type"
  private const val MODE = "mode"
  private const val LANGUAGE = "language"
  private const val PLATFORM = "platform"

  val COURSE_MODE_FIELD = EventFields.Enum<CourseMode>(MODE)

  val ITEM_TYPE_FIELD = EventFields.String(TYPE, listOf(
    // course types
    "CheckiO", "PyCharm", "Coursera", "Hyperskill", "Marketplace", "Codeforces",
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

  val PLATFORM_FIELD = EventFields.String(PLATFORM, listOf(
      "Hyperskill",
      "Stepik",
      "Js_CheckiO",
      "Py_CheckiO",
      "Marketplace",
      "Codeforces"
    )
  )
}
