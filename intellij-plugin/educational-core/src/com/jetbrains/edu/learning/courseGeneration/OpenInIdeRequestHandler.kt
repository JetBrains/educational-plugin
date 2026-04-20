package com.jetbrains.edu.learning.courseGeneration

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.CourseValidationResult

abstract class OpenInIdeRequestHandler<in T : OpenInIdeRequest> {
  @get:DialogTitle
  abstract val courseLoadingProcessTitle: String

  /**
   * Returns the project in which the course was open, or null if the project was not found
   */
  abstract fun openInExistingProject(request: T, findProject: ((Course) -> Boolean) -> Pair<Project, Course>?): Project?

  abstract fun getCourse(request: T, indicator: ProgressIndicator): Result<Course, CourseValidationResult>

  /**
   * Called after the request is handled, and the project is known.
   * Always runs in EDT.
   */
  open fun afterProjectOpened(request: T, project: Project) {
    // do nothing by default
  }
}