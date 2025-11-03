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

  val COURSE_ID_FIELD = EventFields.Int(COURSE_ID, listOf(
    0,      // unpublished local course
    16628,  // https://plugins.jetbrains.com/plugin/16628-kotlin-koans
    16629,  // https://plugins.jetbrains.com/plugin/16629-scala-tutorial
    16630,  // https://plugins.jetbrains.com/plugin/16630-introduction-to-python
    16631,  // https://plugins.jetbrains.com/plugin/16631-learn-rust
    17519,  // https://plugins.jetbrains.com/plugin/17519-amazing
    17654,  // https://plugins.jetbrains.com/plugin/17654-atomickotlin
    18302,  // https://plugins.jetbrains.com/plugin/18302-python-libraries--numpy
    18392,  // https://plugins.jetbrains.com/plugin/18392-machine-learning-101
    18905,  // https://plugins.jetbrains.com/plugin/18905-java-for-beginners
    19933,  // https://plugins.jetbrains.com/plugin/19933-liu-course-java-oop
    20995,  // https://plugins.jetbrains.com/plugin/20995-c-basics
    21005,  // https://plugins.jetbrains.com/plugin/21005-java-programming-basics
    21067,  // https://plugins.jetbrains.com/plugin/21067-kotlin-onboarding-introduction
    21188,  // https://plugins.jetbrains.com/plugin/21188-reinforcement-learning-maze-solver
    21913,  // https://plugins.jetbrains.com/plugin/21913-kotlin-onboarding-object-oriented-programming
    22214,  // https://plugins.jetbrains.com/plugin/22214-algorithmic-challenges-in-kotlin
    22686,  // https://plugins.jetbrains.com/plugin/22686-gateway-to-pandas
    23048,  // https://plugins.jetbrains.com/plugin/23048-practical-ide-code-refactoring-in-kotlin
    23135,  // https://plugins.jetbrains.com/plugin/23135-tour-of-go
    23312,  // https://plugins.jetbrains.com/plugin/23312-coroutines-and-channels
    23369,  // https://plugins.jetbrains.com/plugin/23369-kotlin-onboarding-collections
    23536,  // https://plugins.jetbrains.com/plugin/23536-ui-test-automation-with-selenium-and-python
    23833,  // https://plugins.jetbrains.com/plugin/23833-functional-programming-in-scala
    24051,  // https://plugins.jetbrains.com/plugin/24051-introduction-to-ide-code-refactoring-in-java,
    25097,  // https://plugins.jetbrains.com/plugin/25097-building-a-multicomponent-flask-app
    25212,  // https://plugins.jetbrains.com/plugin/25212-100-days-of-code--the-complete-python-pro-bootcamp
    25398,  // https://plugins.jetbrains.com/plugin/25398-ide-plugin-development-course
    26697,  // https://plugins.jetbrains.com/plugin/26697-introduction-to-javascript-programming
    27682,  // https://plugins.jetbrains.com/plugin/27682-learning-python-in-fragmented-time-basics-
    27687,  // https://plugins.jetbrains.com/plugin/27687-full-stack-javascript-for-beginners
    27805,  // https://plugins.jetbrains.com/plugin/27805-100-exercises-to-learn-rust
    27941,  // https://plugins.jetbrains.com/plugin/27941-data-visualization-in-python
  ))
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
