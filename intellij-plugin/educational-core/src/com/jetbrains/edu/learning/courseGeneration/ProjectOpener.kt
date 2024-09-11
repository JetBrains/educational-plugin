package com.jetbrains.edu.learning.courseGeneration

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.requestFocus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.CourseValidationResult
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.stepik.builtInServer.EduBuiltInServerUtils

abstract class ProjectOpener {

  fun <T: OpenInIdeRequest>open(requestHandler: OpenInIdeRequestHandler<T>, request: T): Result<Boolean, CourseValidationResult> {
    runInEdt {
      // We might perform heavy operations (including network access)
      // So we want to request focus and show progress bar so as it won't seem that IDE doesn't respond
      requestFocus()
    }
    with(requestHandler) {
      val project = openInOpenedProject(request) ?: openInRecentProject(request)
      if (project != null) {
        invokeLater {
          afterProjectOpened(request, project)
        }
        return Ok(true)
      }

      return openInNewProject(request)
    }
  }

  private fun <T: OpenInIdeRequest> OpenInIdeRequestHandler<T>.openInOpenedProject(request: T): Project? =
    openInExistingProject(request, ::focusOpenProject)

  private fun <T: OpenInIdeRequest> OpenInIdeRequestHandler<T>.openInRecentProject(request: T): Project? =
    openInExistingProject(request, EduBuiltInServerUtils::openRecentProject)

  fun <T: OpenInIdeRequest> OpenInIdeRequestHandler<T>.openInNewProject(request: T): Result<Boolean, CourseValidationResult> {
    return computeUnderProgress(title = courseLoadingProcessTitle) { indicator ->
      getCourse(request, indicator)
    }.map { course ->
      getInEdt {
        requestFocus()
        val opened = newProject(course)
        if (opened) {
          val project = course.project ?: openInExistingProject(request, ::focusOpenProject, searchProjectOnly = true)
          if (project != null) {
            afterProjectOpened(request, project)
          }
        }
        opened
      }
    }
  }

  protected abstract fun newProject(course: Course): Boolean

  protected abstract fun focusOpenProject(coursePredicate: (Course) -> Boolean): Pair<Project, Course>?

  companion object {
    fun getInstance(): ProjectOpener = service()
  }
}