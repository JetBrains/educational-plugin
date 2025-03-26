package com.jetbrains.edu.aiHints.core.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.HyperlinkAdapter
import com.intellij.util.asSafely
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesCounterUsageCollector
import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.aiHints.core.HintStateManager
import com.jetbrains.edu.aiHints.core.HintsLoader
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.aiHints.core.ui.EduAiHintsIcons
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduUtilsKt.showPopup
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.actions.EduAIHintsUtils.GET_HINT_ACTION_ID
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.agreement.UserAgreementManager
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import javax.swing.event.HyperlinkEvent

class GetHint : ActionWithProgressIcon() {

  init {
    setUpSpinnerPanel(GET_HINT_ACTION_ID)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    if (!isFeatureEnabled(EduExperimentalFeatures.AI_HINTS)) return
    val project = e.project ?: return
    val course = project.course.asSafely<EduCourse>() ?: return
    val task = project.getCurrentTask() ?: return
    /**
     * Promote the user to accept the AI Agreement in order to access the action
     */
//    if (!UserAgreementSettings.getInstance().aiServiceAgreement) {
//      e.presentation.isVisible = true
//      e.presentation.text = "Accept AI Agreement First"
//      return
//    }
    val isMarketplaceStudyCourse = course.isMarketplace && course.isStudy
    val isFailedEduTask = task is EduTask && task.status == CheckStatus.Failed
    e.presentation.isEnabled = isMarketplaceStudyCourse && isFailedEduTask && EduAIHintsProcessor.forCourse(course) != null
    e.presentation.isVisible = HintStateManager.isDefault(project)
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
    if (!UserAgreementSettings.getInstance().aiServiceAgreement) {
      return e.dataContext.showPromotionPopup(project)
    }

    val taskToolWindow = TaskToolWindowView.getInstance(project)
    val task = taskToolWindow.currentTask ?: return
    taskToolWindow.updateCheckPanel(task)
    EduAIFeaturesCounterUsageCollector.hintButtonClicked(task)
    HintsLoader.getInstance(project).getHint(task)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private fun DataContext.showPromotionPopup(project: Project, htmlContent: String = """Accept <a href="">AI Agreement</a> First""") {
    val balloon = JBPopupFactory.getInstance()
      .createHtmlTextBalloonBuilder(
        htmlContent,
        EduAiHintsIcons.Hint,
        UIUtil.getToolTipActionBackground(),
        AcceptAiAgreementHandler(project)
      ).createBalloon()

    val tooltipRelativePoint = JBPopupFactory.getInstance().guessBestPopupLocation(this)
    balloon.show(tooltipRelativePoint, Balloon.Position.above)
  }

  private inner class AcceptAiAgreementHandler(
    private val project: Project,
  ) : HyperlinkAdapter() {
    override fun hyperlinkActivated(e: HyperlinkEvent) {
      UserAgreementManager.getInstance().showUserAgreement(project)
    }
  }
}