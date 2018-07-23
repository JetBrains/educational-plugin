package com.jetbrains.edu.coursecreator.actions.stepik

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.RemoteCourse


class UpdateAdditionalMaterials: DumbAwareAction("Update Additional Materials") {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return
    if (course !is RemoteCourse) {
      return
    }
    ProgressManager.getInstance().run(object : Task.Modal(project, "Updating Additional Materials", false) {
      override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = false
        CCStepikConnector.updateAdditionalMaterials(project, course.id)
      }
    })
  }

  override fun update(e: AnActionEvent?) {
    val presentation = e?.presentation ?: return
    presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return
    if (course !is RemoteCourse || course.isStudy) {
      return
    }
    presentation.isEnabledAndVisible = true
  }
}