package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProviderEP
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import icons.EducationalCoreIcons
import javax.swing.Icon

class JetBrainsAcademyPlatformProvider : CoursesPlatformProvider {
  override val name: String = EduNames.JBA

  override val icon: Icon get() = EducationalCoreIcons.JB_ACADEMY_TAB

  override val panel get()= JetBrainsAcademyCoursesPanel(this)

  override suspend fun loadCourses(): List<Course> {
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