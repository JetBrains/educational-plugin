package com.jetbrains.edu.kotlin.checker

import com.jetbrains.edu.kotlin.KtCourseBuilder
import com.jetbrains.edu.learning.checker.CheckersTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

abstract class KtCheckersTestBase : CheckersTestBase() {
  override fun getGenerator(course: Course): CourseProjectGenerator<JdkProjectSettings> =
    KtCourseBuilder().getCourseProjectGenerator(course)
}
