package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.OpenTaskOnSiteAction
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.createActionLink
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.THEORY_TAB
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.ListenersAdder
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.CodeHighlighter
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.MediaThemesAndExternalLinkIconsTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.ResourceWrapper
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers.createViewerDependingOnCurrentUILibrary
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator

class TheoryTab(project: Project) : AdditionalTab(project, THEORY_TAB) {

  private val htmlViewer = createViewerDependingOnCurrentUILibrary(
    project,
    MediaThemesAndExternalLinkIconsTransformer then CodeHighlighter then ResourceWrapper then ListenersAdder
  )
  override val innerTextPanel: JComponent
    get() = htmlViewer.component

  init {
    setupTextViewer()
    Disposer.register(this, htmlViewer)
    add(createBottomPanel(), BorderLayout.SOUTH)
  }

  override fun update(task: Task) {
    if (task !is TheoryTask) {
      error("Selected task isn't Theory task")
    }

    val taskHtml = EduUtils.getTaskTextFromTask(project, task) ?: EduCoreBundle.message("label.open.task")
    htmlViewer.setHtmlWithContext(taskHtml, HtmlTransformerContext(project, task))
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