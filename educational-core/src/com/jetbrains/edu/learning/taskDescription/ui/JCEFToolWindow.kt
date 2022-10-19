package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext

class JCEFToolWindow(project: Project) : TaskDescriptionToolWindow(project) {

  private var currentTask: ChoiceTask? = null

  init {
    ApplicationManager.getApplication().messageBus.connect(this)
      .subscribe(LafManagerListener.TOPIC,
                 LafManagerListener { TaskDescriptionView.updateAllTabs(project) })
  }

  override fun updateTaskSpecificPanel(task: Task?) {
    taskSpecificPanelViewer.component.isVisible = false
    if (task !is ChoiceTask) {
      return
    }

    currentTask = task

    taskSpecificPanelViewer.component.preferredSize = JBUI.size(Int.MAX_VALUE, 250)
    taskSpecificPanelViewer.setHtmlWithContext(HtmlTransformerContext(project, task))
    taskSpecificPanelViewer.component.isVisible = true
  }
}
