package com.jetbrains.edu.learning.marketplace.peekSolution

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.actions.CompareWithAnswerAction
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType
import java.awt.FlowLayout
import javax.swing.Box
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
    private fun horizontalGap() = Box.createRigidArea(JBDimension(3, 0))
  }
}