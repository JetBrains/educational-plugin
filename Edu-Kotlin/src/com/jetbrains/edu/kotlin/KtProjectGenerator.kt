package com.jetbrains.edu.kotlin

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.intellij.EduCourseBuilderBase
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator

class KtProjectGenerator(courseBuilder: EduCourseBuilderBase, course: Course)
  : GradleCourseProjectGenerator(courseBuilder, course)
