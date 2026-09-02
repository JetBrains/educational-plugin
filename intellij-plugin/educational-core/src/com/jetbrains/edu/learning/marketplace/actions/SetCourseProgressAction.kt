package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.marketplace.certificate.CourseCertificatePercentageService
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.VisibleForTesting

class SetCourseProgressAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val service = CourseCertificatePercentageService.getInstance(project)
    val input = Messages.showInputDialog(
      project,
      EduCoreBundle.message("action.Educational.Student.SetCourseProgress.message"),
      EduCoreBundle.message("action.Educational.Student.SetCourseProgress.text"),
      null,
      service.completedPercent?.toString().orEmpty(),
      PercentInputValidator
    ) ?: return

    service.completedPercent = input.trim().toIntOrNull()
    thisLogger().info("Completed course percent override is set to ${service.completedPercent}")
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.project?.course != null
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  @VisibleForTesting
  object PercentInputValidator : InputValidatorEx {
    override fun getErrorText(inputString: String): String? {
      // An empty value resets the override
      val value = inputString.trim().ifEmpty { return null }
      val percent = value.toIntOrNull()
                    ?: return EduCoreBundle.message("action.Educational.Student.SetCourseProgress.error.not.a.number")
      if (percent !in 0..100) {
        return EduCoreBundle.message("action.Educational.Student.SetCourseProgress.error.out.of.range")
      }
      return null
    }
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Student.SetCourseProgress"
  }
}
