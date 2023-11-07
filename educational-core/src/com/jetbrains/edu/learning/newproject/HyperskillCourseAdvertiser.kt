package com.jetbrains.edu.learning.newproject

import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProviderEP
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseVisibility
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.wrapWithUtm

/**
 * Fake course type for advertising JBA
 */
class HyperskillCourseAdvertiser : Course() {
  init {
    course.name = "JetBrains Academy Project"
    visibility = CourseVisibility.FeaturedVisibility(1)
    description = """
     Project Information
     
     Each project represents a real-world application, like a chat bot, game, or neural network. To get started:
     - Log in to Hyperskill and select a learning track.
     
     - Pick a project based on your proficiency level.
     
     - Follow your personalized study plan step by step.
     
     Once you complete your project, you can publish it on GitHub right from your IDE.     
     <a href="${wrapWithUtm("https://www.jetbrains.com/academy/", "browse-courses")}">Learn more about how to get started.</a>
   """.trimIndent()
  }

  val supportedLanguages: List<String>
    get() = FEATURED_LANGUAGES.mapNotNull { languageId ->
      CourseCompatibilityProviderEP.find(languageId, DEFAULT_ENVIRONMENT)?.technologyName
    }

  companion object {
    private val FEATURED_LANGUAGES = listOf(
      EduFormatNames.JAVA,
      EduFormatNames.KOTLIN,
      EduFormatNames.PYTHON,
      EduFormatNames.JAVASCRIPT
    )
  }
}