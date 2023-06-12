package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.actions.OpenTaskOnSiteAction
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.createActionLink
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindow.Companion.getTaskDescription
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.TaskDescriptionTransformer
import com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.THEORY_TAB
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JSeparator

class TheoryTab(project: Project) : AdditionalTab(project, THEORY_TAB) {

  init {
    init()
    add(createBottomPanel(), BorderLayout.SOUTH)
  }

  override fun update(task: Task) {
    if (task !is TheoryTask) {
      error("Selected task isn't Theory task")
    }

    val transformerContext = HtmlTransformerContext(project, task, uiMode)
    val plainTaskDescription = getTaskDescription(project, task)
    val html = TaskDescriptionTransformer.transform(plainTaskDescription, transformerContext)
    setText(html)
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