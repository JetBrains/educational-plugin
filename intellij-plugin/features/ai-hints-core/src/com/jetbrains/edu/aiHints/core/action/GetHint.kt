package com.jetbrains.edu.aiHints.core.action

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.ui.GotItTooltip
import com.intellij.ui.HyperlinkAdapter
import com.intellij.util.asSafely
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesCounterUsageCollector
import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.aiHints.core.HintStateManager
import com.jetbrains.edu.aiHints.core.HintsLoader
import com.jetbrains.edu.aiHints.core.context.TaskHintsDataHolder
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
    val task = project.getCurrentTask().asSafely<EduTask>() ?: return

    // Action is not available for tasks with no functions in the Author's Solution
    if (!task.isFunctionsPresented(project)) return

    val isMarketplaceStudyCourse = course.isMarketplace && course.isStudy
    e.presentation.isEnabledAndVisible =
      isMarketplaceStudyCourse
      && task.status == CheckStatus.Failed
      && EduAIHintsProcessor.forCourse(course) != null
      && HintStateManager.isDefault(project)
    e.presentation.putClientProperty(PROJECT_KEY, project)
  }

  private fun EduTask.isFunctionsPresented(project: Project): Boolean {
    val taskHintData = TaskHintsDataHolder.getInstance(project).getTaskHintData(this) ?: return false
    return taskHintData.authorSolutionContext.isFunctionsPresented()
  }

  override fun createCustomComponent(presentation: Presentation, place: String): JButton {
    val button = super.createCustomComponent(presentation, place)
    button.text = presentation.text
    button.isEnabled = presentation.isEnabled
    button.isVisible = presentation.isVisible
    button.isDefault = false
    button.putClientProperty(GET_HINT_BUTTON, true)
    createGotItTooltip(presentation)
    return button
  }

  private fun createGotItTooltip(presentation: Presentation) {
    val project = presentation.getClientProperty(PROJECT_KEY)
    GotItTooltip(
      GOT_IT_TOOLTIP_ID,
      EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.got.it.tooltip.text"),
      parentDisposable = project?.let { StudyTaskManager.getInstance(it) }
    )
      .withHeader(EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.got.it.tooltip.header"))
      .withPosition(Balloon.Position.above)
      .assignTo(presentation, GotItTooltip.TOP_MIDDLE)
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
    private val PROJECT_KEY: Key<Project> = Key.create("project")

    private const val GOT_IT_TOOLTIP_ID: String = "Educational.Hints.GetHint.got.it.tooltip"
  }
}