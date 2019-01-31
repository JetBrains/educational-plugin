package com.jetbrains.edu.java.stepik.hyperskill

import com.jetbrains.edu.java.JCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator

class JHyperskillCourseBuilder : JCourseBuilder() {

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator {
    return JHyperskillCourseProjectGenerator(this, course)
  }
}
