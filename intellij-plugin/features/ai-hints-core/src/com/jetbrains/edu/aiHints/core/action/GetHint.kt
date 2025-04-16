package com.jetbrains.edu.aiHints.core.action

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
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
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.actions.ActionWithButtonCustomComponent
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.agreement.UserAgreementManager
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings.Companion.isJBALoggedIn
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.ui.isDefault
import org.jetbrains.annotations.Nls
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent

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
  }

  override fun createCustomComponent(presentation: Presentation, place: String): JButton {
    val button = super.createCustomComponent(presentation, place)
    button.text = presentation.text
    button.isEnabled = presentation.isEnabled
    button.isVisible = presentation.isVisible
    button.isDefault = false
    button.putClientProperty(GET_HINT_BUTTON, true)
    return button
  }

  override fun updateCustomComponent(component: JComponent, presentation: Presentation) {
    super.updateCustomComponent(component, presentation)
    val button = component.asSafely<JButton>() ?: return
    button.isDefault = false
    button.invalidate()
    button.repaint()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (!isJBALoggedIn()) {
      val popup = createPopup(project, EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.popup.login")) {
        MarketplaceConnector.getInstance().doAuthorize()
      }
      popup.show(e.dataContext)
      return
    }
    if (!UserAgreementSettings.getInstance().aiServiceAgreement) {
      val popup = createPopup(project, EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.popup.agreement")) {
        UserAgreementManager.getInstance().showUserAgreement(project)
      }
      popup.show(e.dataContext)
      return
    }
    if (HintsLoader.isRunning(project)) {
      e.dataContext.showPopup(EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.already.in.progress"))
      return
    }
    val taskToolWindow = TaskToolWindowView.getInstance(project)
    val task = taskToolWindow.currentTask ?: return
    taskToolWindow.updateCheckPanel(task)
    EduAIFeaturesCounterUsageCollector.hintButtonClicked(task)
    HintsLoader.getInstance(project).getHint(task)
  }

  private fun createPopup(project: Project, @Nls text: String, action: Runnable): Balloon {
    val popupParentDisposable = Disposer.newDisposable()
    // Ensuring the parent disposable is disposed even when the hyperlink is not clicked
    Disposer.register(StudyTaskManager.getInstance(project), popupParentDisposable)

    return JBPopupFactory.getInstance()
      .createHtmlTextBalloonBuilder(
        text,
        EduAiHintsIcons.Hint,
        UIUtil.getToolTipActionBackground(),
        object : HyperlinkAdapter() {
          override fun hyperlinkActivated(hyperlinkEvent: HyperlinkEvent) {
            // Close the popup once the hyperlink is clicked
            Disposer.dispose(popupParentDisposable)
            action.run()
          }
        }
      )
      .setDisposable(popupParentDisposable)
      .createBalloon()
  }

  private fun Balloon.show(dataContext: DataContext) {
    val component = dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT).asSafely<JComponent>()
    val getHintButton = UIUtil.findComponentsOfType(component, JButton::class.java)
      .firstOrNull { it.getClientProperty(GET_HINT_BUTTON) == true }
    val relativePoint = if (getHintButton != null) {
      JBPopupFactory.getInstance().guessBestPopupLocation(getHintButton)
    }
    else {
      JBPopupFactory.getInstance().guessBestPopupLocation(dataContext)
    }
    show(relativePoint, Balloon.Position.above)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {
    private val GET_HINT_BUTTON: Key<Boolean> = Key.create("getHintButton")
  }
}