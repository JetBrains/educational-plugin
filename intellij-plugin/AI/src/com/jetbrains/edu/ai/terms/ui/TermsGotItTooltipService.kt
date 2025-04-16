package com.jetbrains.edu.ai.terms.ui

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.ui.GotItTooltip
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.canShowTerms
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.DescriptionTab
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType

@Service(Service.Level.PROJECT)
class TermsGotItTooltipService(private val project: Project) {
  private var gotItTooltip: GotItTooltip? = null

  fun showTermsGotItTooltip() {
    val currentTask = project.getCurrentTask() ?: return
    showTermsGotItTooltip(currentTask)
  }

  fun showTermsGotItTooltip(task: Task) {
    removeExistingTooltip()

  	// some tasks may not have terms, so we don't want to show tooltips for them
  	// because of that we should validate that a task has terms
    if (!canShowTerms(project, task)) return
    if (TermsProjectSettings.getInstance(project).getTaskTerms(task) == null) return

    val taskToolWindowView = TaskToolWindowView.getInstance(project)
    if (!taskToolWindowView.isSelectedTab(TabType.DESCRIPTION_TAB)) return
    val descriptionTab = taskToolWindowView.getTab(TabType.DESCRIPTION_TAB) as? DescriptionTab ?: return

    val tooltip = GotItTooltip(TERMS_GOT_IT_TOOLTIP_ID, EduAIBundle.message("ai.terms.tooltip.text"), descriptionTab)
      .withHeader(EduAIBundle.message("ai.terms.tooltip.header"))
      .withPosition(Balloon.Position.atLeft)
    tooltip.show(descriptionTab, GotItTooltip.LEFT_MIDDLE)
  }

  private fun removeExistingTooltip() {
    gotItTooltip?.let { Disposer.dispose(it) }
    gotItTooltip = null
  }

  companion object {
    private const val TERMS_GOT_IT_TOOLTIP_ID = "edu.ai.theory.lookup.tooltip"

    fun getInstance(project: Project): TermsGotItTooltipService = project.service()
  }
}