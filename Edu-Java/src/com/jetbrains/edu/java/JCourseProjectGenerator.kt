package com.jetbrains.edu.java

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.intellij.EduCourseBuilderBase
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator

class JCourseProjectGenerator(courseBuilder: EduCourseBuilderBase, course: Course)
  : GradleCourseProjectGenerator(courseBuilder, course)
