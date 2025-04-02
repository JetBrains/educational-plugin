package com.jetbrains.edu.aiHints.core.action

import com.intellij.ide.HelpTooltip
import com.intellij.ide.HelpTooltipManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbService
import com.intellij.util.asSafely
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesCounterUsageCollector
import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.aiHints.core.HintStateManager
import com.jetbrains.edu.aiHints.core.HintsLoader
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduUtilsKt.showPopup
import com.jetbrains.edu.learning.actions.ActionWithButtonCustomComponent
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.agreement.UserAgreementManager
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.ui.isDefault
import javax.swing.JButton

class GetHint : ActionWithButtonCustomComponent() {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    if (!isFeatureEnabled(EduExperimentalFeatures.AI_HINTS)) return
    val project = e.project ?: return
    val course = project.course.asSafely<EduCourse>() ?: return
    val task = project.getCurrentTask() ?: return

    val isMarketplaceStudyCourse = course.isMarketplace && course.isStudy
    val isFailedEduTask = task is EduTask && task.status == CheckStatus.Failed
    e.presentation.isEnabledAndVisible =
      isMarketplaceStudyCourse && isFailedEduTask && EduAIHintsProcessor.forCourse(course) != null && HintStateManager.isDefault(project)

    // If action is visible but AI agreement is not accepted, then it should not be enabled
    if (!UserAgreementSettings.getInstance().aiServiceAgreement && e.presentation.isVisible) {
      e.presentation.isEnabled = false
    }
  }

  override fun createCustomComponent(presentation: Presentation, place: String): JButton {
    val button = super.createCustomComponent(presentation, place)
    button.text = presentation.text
    button.isEnabled = presentation.isEnabled
    button.isVisible = presentation.isVisible
    button.isDefault = false
    button.installAIAgreementTooltip(presentation.isVisible)
    return button
  }

  @Suppress("DialogTitleCapitalization")
  private fun JButton.installAIAgreementTooltip(isButtonVisible: Boolean) {
    HelpTooltip()
      .setTitle(EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.text"))
      .setDescription(EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.tooltip.description"))
      .setLink(EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.tooltip.link")) {
        val project = ActionToolbar.getDataContextFor(this).getData(CommonDataKeys.PROJECT)
        if (project != null) {
          UserAgreementManager.getInstance().showUserAgreement(project)
        }
      }
      .installOn(this)

    HelpTooltipManager.setMasterPopupOpenCondition(this) {
      // Show tooltip when AI agreement is not accepted
      isButtonVisible && !UserAgreementSettings.getInstance().aiServiceAgreement
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (DumbService.isDumb(project)) {
      @Suppress("DEPRECATION") // BACKCOMPAT: 2024.2 Use [ActionUtil.getActionUnavailableMessage]
      return e.dataContext.showPopup(ActionUtil.getUnavailableMessage(templateText, false))
    }
    if (HintsLoader.isRunning(project)) {
      return e.dataContext.showPopup(EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.already.in.progress"))
    }

    val taskToolWindow = TaskToolWindowView.getInstance(project)
    val task = taskToolWindow.currentTask ?: return
    taskToolWindow.updateCheckPanel(task)
    EduAIFeaturesCounterUsageCollector.hintButtonClicked(task)
    HintsLoader.getInstance(project).getHint(task)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}