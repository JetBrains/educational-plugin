package com.jetbrains.edu.learning.newproject

import com.intellij.lang.Language
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseVisibility

/**
 * Fake course type for advertising JBA
 */
class JetBrainsAcademyCourse(language: Language) : Course() {
  init {
    course.language = language.id
    course.name = "JetBrains Academy ${language.displayName} Track"
    visibility = CourseVisibility.FeaturedVisibility(1)
    description = """
     Learn to program by creating working applications:
     
     - Choose a project and get a personal curriculum with all the concepts necessary to build it
     
     - See how it's all related with the Knowledge Map
     
     - Develop projects and solve coding tasks with professional IDEs by JetBrains
     
     JetBrains Academy experience starts in your browser 
     
     <a href="https://hi.hyperskill.org?utm_source=ide&utm_content=browse-courses">Learn more</a>
   """.trimIndent()
  }
}