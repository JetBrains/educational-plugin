package com.jetbrains.edu.coursecreator.actions.stepik

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.coursecreator.actions.CCNewCourseActionBase
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.stepik.StepikConnector

class CCGetCourseFromStepik : CCNewCourseActionBase("Get Course From Stepik", "Get Course From Stepik") {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val courseId = Messages.showInputDialog("Please, enter course id", "Get Course From Stepik", null)
    if (!courseId.isNullOrEmpty()) {
      ProgressManager.getInstance().run(object : Task.Modal(project, "Loading Course", true) {
        override fun run(indicator: ProgressIndicator) {
          createCourse(project, courseId!!)
        }
      })
    }
  }

  private fun createCourse(project: Project?, courseId: String) {
    val info = CCStepikConnector.getCourseInfo(courseId)
    if (info == null) {
      showError(courseId)
      return
    }
    val course = StepikConnector.getCourse(project, info)
    if (course == null) {
      showError(courseId)
      return
    }
    ApplicationManager.getApplication().invokeLater {
      CCNewCourseDialog("Get Course From Stepik", "Create", course, this::initializeCourseProject).show()
    }
  }

  private fun showError(courseId: String) {
    ApplicationManager.getApplication().invokeLater {
      Messages.showWarningDialog("Can't load course info. Check that course with `$courseId` id exists", "Failed to Load Course")
    }
  }
}
