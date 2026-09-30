package com.jetbrains.edu.python.learning.newproject

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EnvironmentSettingKey
import com.jetbrains.edu.learning.courseFormat.getEnvironmentSetting
import com.jetbrains.python.psi.LanguageLevel

/**
 * The map with the python versions for which we know the course is not working.
 *
 * Such a map is a temporary solution, if a course gets the environment setting MAX_SUPPORTED_PYTHON_LANGUAGE_VERSION,
 * it should be removed from this map
 */
private val MAX_SUPPORTED_PYTHON_LANGUAGE_VERSION: Map<Int, LanguageLevel> = mapOf(
  28816 /* Mastering Large Language Models */ to LanguageLevel.PYTHON313,
  205112 /* AWS, same*/ to LanguageLevel.PYTHON313,

  25097 /* Building a multicomponent Flask app / Building a Flask App with Microservices */ to LanguageLevel.PYTHON312,
  205109 /*AWS* same */ to LanguageLevel.PYTHON312,

  27941 /* Data Visualization with Python */ to LanguageLevel.PYTHON314,
  22686 /* Gateway to Pandas / Mastering Python Libraries – Pandas */ to LanguageLevel.PYTHON314,
  23986 /* Master AI: Build Game Players using AlphaZero */ to LanguageLevel.PYTHON312,
)

fun isVersionTooNewForCourse(course: Course, sdkLanguageLevel: LanguageLevel): Boolean {
  val maxPythonVersion = course.getMaxSupportedPythonLanguageLevel() ?: return false
  return sdkLanguageLevel > maxPythonVersion
}

/**
 * `null` means "no max version restrictions"
 */
fun Course.getMaxSupportedPythonLanguageLevel(): LanguageLevel? {
  val maxSupportedPythonVersionFromEnvironmentSettings =
    getEnvironmentSetting(MAX_PYTHON_LANGUAGE_VERSION)
    ?.parseLanguageLevel()

  return maxSupportedPythonVersionFromEnvironmentSettings ?:
         // TODO remove after the MAX_SUPPORTED_PYTHON_VERSION becomes empty
         MAX_SUPPORTED_PYTHON_LANGUAGE_VERSION[course.id]
}

/**
 * Parses python language levels of the form Major.Minor, such as "3.14", does not parse other versions such as "3.14.5"
 */
private fun String.parseLanguageLevel(): LanguageLevel? {
  return LanguageLevel.fromPythonVersionSafe(this)
    ?.takeIf { it.toPythonVersion() == this }
}

val MAX_PYTHON_LANGUAGE_VERSION = EnvironmentSettingKey("max_python_language_version")
