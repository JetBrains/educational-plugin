package com.jetbrains.edu.learning.checkio.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator
import com.jetbrains.edu.learning.checkio.CheckiOCourseUpdater
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.notifications.errors.handlers.CheckiOErrorHandler
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task

abstract class CheckiOCheckListener(
  private val contentGenerator: CheckiOCourseContentGenerator,
  private val oAuthConnector: CheckiOOAuthConnector
) : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (task.course is CheckiOCourse && result.status == CheckStatus.Solved) {
      val course = task.course as CheckiOCourse

      if (isEnabledForCourse(course)) {
        val courseUpdater = CheckiOCourseUpdater(course, project, contentGenerator)
        try {
          courseUpdater.doUpdate()
        }
        catch (e: Exception) {
          CheckiOErrorHandler(
            "Failed to update the course",
            oAuthConnector
          ).handle(e)
        }
      }
    }
  }

  protected abstract fun isEnabledForCourse(course: CheckiOCourse): Boolean
}