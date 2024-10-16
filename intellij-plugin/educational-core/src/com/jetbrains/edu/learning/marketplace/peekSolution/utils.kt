package com.jetbrains.edu.learning.marketplace.peekSolution

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.learning.actions.CompareWithAnswerAction
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.submissions.SubmissionsListener
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskToolWindow.ui.LightColoredActionLink
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent

/**
 * Function is used to create a panel with a link to Community solutions when solution by author is hidden.
 *
 * @see [com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckDetailsPanel]
 */
fun createCommunityLinksPanel(project: Project, task: Task): DialogPanel {
  val panel = panel {
    row(EduCoreBundle.message("submissions.got.stuck")) {
      link(EduCoreBundle.message("submissions.see.community.solutions.link")) {
        val taskToolWindow = TaskToolWindowView.getInstance(project)
        val isCommunityPanelShowing = taskToolWindow.isSelectedTab(TabType.SUBMISSIONS_TAB) && taskToolWindow.isCommunityTabShowing()
        if (!isCommunityPanelShowing) {
          taskToolWindow.showCommunityTab()
          taskToolWindow.selectTab(TabType.SUBMISSIONS_TAB)
        }
        EduCounterUsageCollector.communityTabOpenedByLink(!isCommunityPanelShowing)
      }
    }
  }
  panel.isVisible = false
  listenCommunitySubmissions(project, task, panel)
  return panel
}

fun marketplacePeekSolutionLink(isSolved: Boolean): LightColoredActionLink? = if (isSolved) {
  LightColoredActionLink(
    @Suppress("DialogTitleCapitalization") EduCoreBundle.message("label.peek.solution"),
    ActionManager.getInstance().getAction(CompareWithAnswerAction.ACTION_ID)
  )
}
else {
  null
}

internal fun listenCommunitySubmissions(project: Project, task: Task, component: JComponent) {
  project.messageBus.connect().subscribe(SubmissionsManager.TOPIC, SubmissionsListener {
    CompletableFuture.runAsync({
      val submissionsManager = SubmissionsManager.getInstance(project)
      if (!submissionsManager.isAllowedToLoadCommunitySolutions(task)) {
        component.isVisible = false
        return@runAsync
      }
      if (!submissionsManager.isCommunitySolutionsLoaded(task)) {
        submissionsManager.loadCommunitySubmissions(task)
      }
      project.invokeLater {
        component.isVisible = true
      }
    }, ProcessIOExecutorService.INSTANCE)
  })
}
