package com.jetbrains.edu.coursecreator.actions.stepik

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.messages.EduCoreActionBundle
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduCoreErrorBundle
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader

@Suppress("ComponentNotRegistered") // registered in educational-core.xml
class CCGetCourseFromStepik : DumbAwareAction(
  EduCoreActionBundle.lazyMessage("action.get.course.text", StepikNames.STEPIK),
  EduCoreActionBundle.lazyMessage("action.get.course.description", StepikNames.STEPIK),
  null) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val courseId = Messages.showInputDialog(
      EduCoreActionBundle.message("action.get.course.enter.course.id"),
      EduCoreActionBundle.message("action.get.course.text", StepikNames.STEPIK),
      null
    )
    if (!courseId.isNullOrEmpty()) {
      ProgressManager.getInstance().run(object : Task.Modal(project, EduCoreActionBundle.message("action.get.course.loading"), true) {
        override fun run(indicator: ProgressIndicator) {
          createCourse(courseId)
        }
      })
    }
  }

  private fun createCourse(courseId: String) {
    val info = StepikConnector.getInstance().getCourseInfo(Integer.valueOf(courseId))
    if (info == null) {
      showError(courseId)
      return
    }

    StepikCourseLoader.loadCourseStructure(info)
    runInEdt {
      CCNewCourseDialog(
        EduCoreActionBundle.message("action.get.course.text", StepikNames.STEPIK),
        EduCoreBundle.message("label.create"),
        info
      ).show()
    }
  }

  private fun showError(courseId: String) {
    runInEdt {
      Messages.showWarningDialog(
        EduCoreErrorBundle.message("failed.to.load.course.not.exists", courseId),
        EduCoreErrorBundle.message("failed.to.load.course")
      )
    }
  }
}
