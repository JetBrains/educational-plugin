package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course

/**
 * Extension point for processing course metadata parameters received from `openCourse` command
 */
interface CourseMetadataProcessor<T> {
  /**
   * Looks for applicable parameters from [rawMetadata] passed during course opening.
   * Returns applicable metadata parameters in structured form or null if the parameters are not applicable for this processor.
   */
  fun findApplicableMetadata(rawMetadata: Map<String, String>): T?

  /**
   * Processes the [metadata] collected by [findApplicableMetadata] for the given [course].
   * Used for persisting data, not for actual heavy tasks.
   */
  fun processMetadata(project: Project, course: Course, metadata: T, courseProjectState: CourseProjectState)

  companion object {
    val EP_NAME = ExtensionPointName.create<CourseMetadataProcessor<*>>("Educational.courseMetadataProcessor")

    private fun <T> CourseMetadataProcessor<T>.process(
      project: Project,
      course: Course,
      rawMetadata: Map<String, String>,
      courseProjectState: CourseProjectState
    ) {
      val metadata = findApplicableMetadata(rawMetadata) ?: return
      processMetadata(project, course, metadata, courseProjectState)
    }

    fun applyProcessors(project: Project, course: Course, rawMetadata: Map<String, String>, courseProjectState: CourseProjectState) {
      thisLogger().info("Applying course metadata processors for course ${course.name} in state $courseProjectState: $rawMetadata")

      EP_NAME.extensions.forEach { processor ->
        processor.process(project, course, rawMetadata, courseProjectState)
      }
    }
  }
}

enum class CourseProjectState {
  CREATED_PROJECT,
  OPENED_PROJECT,
  FOCUSED_OPEN_PROJECT
}