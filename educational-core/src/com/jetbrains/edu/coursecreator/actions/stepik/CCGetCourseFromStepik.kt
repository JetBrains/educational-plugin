package com.jetbrains.edu.coursecreator.actions.stepik

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader

@Suppress("ComponentNotRegistered") // registered in educational-core.xml
class CCGetCourseFromStepik : DumbAwareAction("Get Course From Stepik", "Get Course From Stepik", null) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val courseId = Messages.showInputDialog("Please, enter course id", "Get Course From Stepik", null)
    if (!courseId.isNullOrEmpty()) {
      ProgressManager.getInstance().run(object : Task.Modal(project, "Loading Course", true) {
        override fun run(indicator: ProgressIndicator) {
          createCourse(courseId!!)
        }
      })
    }
  }

  private fun createCourse(courseId: String) {
    val info = CCStepikConnector.getCourseInfo(courseId)
    if (info == null) {
      showError(courseId)
      return
    }

    StepikCourseLoader.loadCourseStructure(info)
    runInEdt {
      CCNewCourseDialog("Get Course From Stepik", "Create", info).show()
    }
  }

  private fun showError(courseId: String) {
    runInEdt {
      Messages.showWarningDialog("Can't load course info. Check that course with `$courseId` id exists", "Failed to Load Course")
    }
  }
}
