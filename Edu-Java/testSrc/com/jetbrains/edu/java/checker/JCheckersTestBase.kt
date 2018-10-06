package com.jetbrains.edu.java.checker

import com.jetbrains.edu.java.learning.JCourseBuilder
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.gradle.JdkProjectSettings
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

abstract class JCheckersTestBase : CheckersTestBase() {
  override fun getGenerator(course: Course): CourseProjectGenerator<JdkProjectSettings> =
    JCourseBuilder().getCourseProjectGenerator(course)
}
