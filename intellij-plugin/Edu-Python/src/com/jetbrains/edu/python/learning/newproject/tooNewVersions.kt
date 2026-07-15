package com.jetbrains.edu.python.learning.newproject

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.python.psi.LanguageLevel

/**
 * The map with the python versions for which we know the course is not working.
 * TODO Such a map is a temporary solution until we implement the way for course authors to specify this version explicitly: See EDU-8982
 */
private val FIRST_UNSUPPORTED_PYTHON_VERSION: Map<Int, LanguageLevel> = mapOf(
  28816 /* Mastering Large Language Models */ to LanguageLevel.PYTHON314,
  205112 /* AWS, same*/ to LanguageLevel.PYTHON314,

  25097 /* Building a multicomponent Flask app / Building a Flask App with Microservices */ to LanguageLevel.PYTHON313,
  205109 /*AWS* same */ to LanguageLevel.PYTHON313,

  27941 /* Data Visualization with Python */ to LanguageLevel.PYTHON315,
  22686 /* Gateway to Pandas / Mastering Python Libraries – Pandas */ to LanguageLevel.PYTHON315,
  23986 /* Master AI: Build Game Players using AlphaZero */ to LanguageLevel.PYTHON313,
)

fun isVersionTooNewForCourse(course: Course, sdkLanguageLevel: LanguageLevel): Boolean {
  val maxPythonVersion = getFirstUnsupportedPythonVersion(course) ?: return false
  return sdkLanguageLevel >= maxPythonVersion
}

/**
 * `null` means "no max version restrictions"
 */
fun getFirstUnsupportedPythonVersion(course: Course): LanguageLevel? = FIRST_UNSUPPORTED_PYTHON_VERSION[course.id]
