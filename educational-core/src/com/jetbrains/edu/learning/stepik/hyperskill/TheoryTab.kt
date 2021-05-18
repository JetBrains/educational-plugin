package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.actions.OpenTaskOnSiteAction
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.createActionLink
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindow.Companion.getTaskDescriptionWithCodeHighlighting
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabManager.TabType.THEORY_TAB
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JSeparator

class TheoryTab(project: Project, theoryTask: TheoryTask) : AdditionalTab(project, THEORY_TAB) {

  init {
    addHyperlinkListener(EduBrowserHyperlinkListener.INSTANCE)

    val text = getTaskDescriptionWithCodeHighlighting(project, theoryTask)
    setText(text, plain = false)

    panel.add(createBottomPanel(), BorderLayout.SOUTH)
  }

  private fun createBottomPanel(): JPanel {
    val actionLink = createActionLink(EduCoreBundle.message("action.open.on.text", EduNames.JBA), OpenTaskOnSiteAction.ACTION_ID, 10, 3)

    val bottomPanel = JPanel(BorderLayout()).apply {
      border = JBUI.Borders.empty(8, 15, 15, 15)
      add(JSeparator(), BorderLayout.NORTH)
      add(actionLink, BorderLayout.CENTER)
    }

    UIUtil.setBackgroundRecursively(bottomPanel, TaskDescriptionView.getTaskDescriptionBackgroundColor())
    return bottomPanel
  }
}