package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course

/**
 * Extension point for processing course parameters received from `openCourse` command
 */
interface CourseParamsProcessor<T> {
  /**
   * Extracts the given course parameters and checks if the processor is applicable to them.
   * Returns null if the parameters are not applicable for this processor.
   */
  fun findApplicableContext(params: Map<String, String>): T?

  /**
   * Processes the [context] collected by [findApplicableContext] for the given [course].
   * Used for persisting data, not for actual heavy tasks.
   */
  fun processCourseParams(project: Project, course: Course, context: T)

  companion object {
    val EP_NAME = ExtensionPointName.create<CourseParamsProcessor<*>>("Educational.courseParamsProcessor")

    private fun <T> CourseParamsProcessor<T>.process(project: Project, course: Course, params: Map<String, String>) {
      val params = findApplicableContext(params) ?: return
      processCourseParams(project, course, params)
    }

    fun applyProcessors(project: Project, course: Course, params: Map<String, String>) {
      EP_NAME.extensions.forEach { processor ->
        processor.process(project, course, params)
      }
    }
  }
}