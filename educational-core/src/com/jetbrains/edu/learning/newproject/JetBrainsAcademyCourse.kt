package com.jetbrains.edu.learning.newproject

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProviderEP
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseVisibility
import com.jetbrains.edu.learning.stepik.hyperskill.wrapWithUtm

/**
 * Fake course type for advertising JBA
 */
class JetBrainsAcademyCourse : Course() {
  init {
    course.name = "JetBrains Academy Track"
    visibility = CourseVisibility.FeaturedVisibility(1)
    programmingLanguage = "NoLanguage" // to avoid accidental usage of default value - Python
    description = """
     Learn to program by creating working applications:
     
     - Choose a project and get a personal curriculum with all the concepts necessary to build it
     
     - See how it's all related with the Knowledge Map
     
     - Develop projects and solve coding tasks with professional IDEs by JetBrains
     
     JetBrains Academy experience starts in your browser 
     
     <a href="${wrapWithUtm("https://www.jetbrains.com/academy/", "browse-courses")}">Learn more</a>
   """.trimIndent()
  }

  val supportedLanguages: List<String>
    get() = FEATURED_LANGUAGES.mapNotNull { languageId ->
      CourseCompatibilityProviderEP.find(languageId, DEFAULT_ENVIRONMENT)?.technologyName
    }

  override val isViewAsEducatorEnabled: Boolean
    get() = false

  companion object {
    private val FEATURED_LANGUAGES = listOf(
      EduNames.JAVA,
      EduNames.KOTLIN,
      EduNames.PYTHON,
      EduNames.JAVASCRIPT
    )
  }
}