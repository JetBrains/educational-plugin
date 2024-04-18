package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.actions.OpenTaskOnSiteAction
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.createActionLink
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindow.Companion.getTaskDescription
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.THEORY_TAB
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TaskToolWindowTextTab
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JSeparator

/**
 * Constructor is called exclusively in [com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabManager.createTab]
 * and MUST NOT be called in any other places
 */
class TheoryTab(project: Project) : TaskToolWindowTextTab(project, THEORY_TAB) {

  init {
    init()
    add(createBottomPanel(), BorderLayout.SOUTH)
  }

  override fun update(task: Task) {
    if (task !is TheoryTask) {
      error("Selected task isn't Theory task")
    }

    setText(getTaskDescription(project, task, uiMode))
  }

  private fun createBottomPanel(): JPanel {
    val actionLink = createActionLink(EduCoreBundle.message("action.open.on.text", EduNames.JBA), OpenTaskOnSiteAction.ACTION_ID, 10, 3)

    val bottomPanel = JPanel(BorderLayout()).apply {
      border = JBUI.Borders.empty(8, 15, 15, 15)
      add(JSeparator(), BorderLayout.NORTH)
      add(actionLink, BorderLayout.CENTER)
    }

    UIUtil.setBackgroundRecursively(bottomPanel, TaskToolWindowView.getTaskDescriptionBackgroundColor())
    return bottomPanel
  }
}