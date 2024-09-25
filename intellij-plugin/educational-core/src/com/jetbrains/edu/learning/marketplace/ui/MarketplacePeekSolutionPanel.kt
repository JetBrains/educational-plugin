package com.jetbrains.edu.learning.marketplace.ui

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.ActionLink
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.actions.CompareWithAnswerAction
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.submissions.SubmissionsListener
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType
import java.awt.FlowLayout
import java.util.concurrent.CompletableFuture
import javax.swing.Box
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class MarketplacePeekSolutionPanel(project: Project, task: Task) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {

  init {
    border = JBUI.Borders.emptyTop(5)

    add(JLabel(EduCoreBundle.message("label.got.stuck")))
    add(horizontalGap())
    add(ActionLink(EduCoreBundle.message("label.peek.authors.solution")) {
      ActionUtil.invokeAction(
        EduActionUtils.getAction(CompareWithAnswerAction.ACTION_ID),
        it.source as ActionLink,
        ActionPlaces.UNKNOWN,
        null,
        null
      )
    })
    val communityLink = JPanel(layout).apply {
      add(horizontalGap())
      add(JLabel(EduCoreBundle.message("label.or.see")))
      add(horizontalGap())
      add(ActionLink(EduCoreBundle.message("label.community.ones")) {
        val taskToolWindow = TaskToolWindowView.getInstance(project)
        taskToolWindow.showCommunityTab()
        taskToolWindow.selectTab(TabType.SUBMISSIONS_TAB)
      })
      isVisible = false
    }
    add(communityLink)
    add(JLabel(EduCoreBundle.message("label.dot")))

    listenCommunitySubmissions(project, task, communityLink)
  }

  companion object {
    fun listenCommunitySubmissions(project: Project, task: Task, component: JComponent) {
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

    /**
     * Function is used to create a panel with a link to shared solutions for non-peek solution courses.
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

    private fun horizontalGap() = Box.createRigidArea(JBDimension(3, 0))
  }
}