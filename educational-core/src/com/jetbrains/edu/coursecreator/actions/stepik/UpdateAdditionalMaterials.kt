package com.jetbrains.edu.coursecreator.actions.stepik

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.FAILED_TITLE
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.showErrorNotification
import com.jetbrains.edu.learning.EduUtils.showNotification
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse

@Suppress("ComponentNotRegistered") // educational-core.xml
class UpdateAdditionalMaterials : DumbAwareAction("Update Additional Materials") {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
    if (!course.isRemote) {
      return
    }
    ProgressManager.getInstance().run(object : Task.Modal(project, "Updating Additional Materials", false) {
      override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = false
        val success = CCStepikConnector.updateAdditionalMaterials(project, course)
        if (!success) {
          showErrorNotification(project, FAILED_TITLE, "Failed to update additional materials")
        }
        else {
          showNotification(project, "Additional materials updated", null)
        }
      }
    })
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return
    if (!course.isRemote || course.isStudy) {
      return
    }
    presentation.isEnabledAndVisible = true
  }
}