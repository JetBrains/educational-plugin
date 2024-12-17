package com.jetbrains.edu.learning.marketplace.peekSolution

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Panel with a link to Community solutions when solution by author is hidden.
 *
 * @see [com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckDetailsPanel]
 */
class LinkToCommunitySolutionsPanel(project: Project, task: Task): JPanel(BorderLayout()) {
  init {
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
    add(panel, BorderLayout.CENTER)
  }
}