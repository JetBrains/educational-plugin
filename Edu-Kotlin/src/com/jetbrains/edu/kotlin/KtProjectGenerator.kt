package com.jetbrains.edu.kotlin

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator

class KtProjectGenerator(course: Course) : GradleCourseProjectGenerator(course) {

  override fun initializeFirstTask(task: Task) {
    KtCourseBuilder.initTask(task)
  }
}
