package com.jetbrains.edu.kotlin

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.intellij.EduCourseBuilderBase
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator

class KtProjectGenerator(courseBuilder: EduCourseBuilderBase, course: Course)
  : GradleCourseProjectGenerator(courseBuilder, course) {

  override fun initializeFirstTask(task: Task) {
    KtCourseBuilder.initTask(task)
  }
}
