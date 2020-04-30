package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProviderEP
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse

class HyperskillCoursesProvider : CoursesProvider {
  override fun loadCourses(): List<Course> {
    return SUPPORTED_LANGUAGES.mapNotNull { languageId ->
      val provider = CourseCompatibilityProviderEP.find(languageId, EduNames.DEFAULT_ENVIRONMENT) ?: return@mapNotNull null
      JetBrainsAcademyCourse(languageId, provider.technologyName)
    }
  }

  companion object {
    private val SUPPORTED_LANGUAGES = listOf(
      EduNames.JAVA,
      EduNames.KOTLIN,
      EduNames.PYTHON
    )
  }
}