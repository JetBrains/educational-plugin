package com.jetbrains.edu.commandLine.processors

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course

/**
 * Extension point for processing course parameters received from `openCourse` command
 * Example: [LtiCourseParamsProcessor]
 * @see com.jetbrains.edu.commandLine.EduOpenCourseAppStarter
 */
interface CourseParamsProcessor<T> {
  fun shouldApply(project: Project, course: Course, params: Map<String, String?>): T?
  fun processCourseParams(project: Project, course: Course, params: T): Boolean

  companion object {
    val EP_NAME = ExtensionPointName.create<CourseParamsProcessor<*>>("Educational.courseParamsProcessor")

    private fun <T> CourseParamsProcessor<T>.process(project: Project, course: Course, params: Map<String, String?>): Boolean {
      val params = shouldApply(project, course, params) ?: return false
      return processCourseParams(project, course, params)
    }

    fun applyProcessors(project: Project, course: Course, params: Map<String, String?>) {
      EP_NAME.extensions.forEach { processor ->
        processor.process(project, course, params)
      }
    }
  }
}