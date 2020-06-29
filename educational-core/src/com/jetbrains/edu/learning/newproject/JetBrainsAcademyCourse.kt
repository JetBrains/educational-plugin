package com.jetbrains.edu.learning.newproject

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseVisibility

/**
 * Fake course type for advertising JBA
 */
class JetBrainsAcademyCourse(languageId: String, trackName: String) : Course() {
  init {
    course.language = languageId
    course.name = "JetBrains Academy $trackName Track"
    visibility = CourseVisibility.FeaturedVisibility(1)
    description = """
     Learn to program by creating working applications:
     
     - Choose a project and get a personal curriculum with all the concepts necessary to build it
     
     - See how it's all related with the Knowledge Map
     
     - Develop projects and solve coding tasks with professional IDEs by JetBrains
     
     JetBrains Academy experience starts in your browser 
     
     <a href="https://www.jetbrains.com/academy/?utm_source=ide&utm_content=browse-courses">Learn more</a>
   """.trimIndent()
  }

  override fun isViewAsEducatorEnabled(): Boolean = false
}