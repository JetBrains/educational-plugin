package com.jetbrains.edu.commandLine.processors

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course

/**
 * Extension point for processing course parameters received from toolbox
 *
 * To support new course parameters, a new child class should be created and registered via the `Educational.courseParamsProcessor`
 * Example: [CourseraCourseParamsProcessor]
 */
interface CourseParamsProcessor {
  fun shouldApply(project: Project, course: Course, params: Map<String, String>): Boolean
  fun processCourseParams(project: Project, course: Course, params: Map<String, String>): Boolean

  companion object {
    val EP_NAME = ExtensionPointName.create<CourseParamsProcessor>("Educational.courseParamsProcessor")
  }
}