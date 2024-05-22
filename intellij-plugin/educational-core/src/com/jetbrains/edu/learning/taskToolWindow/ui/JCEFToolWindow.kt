package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.ui.jcef.TaskInfoJBCefBrowser
import com.jetbrains.edu.learning.taskToolWindow.ui.jcef.TaskSpecificJBCefBrowser
import com.jetbrains.edu.learning.taskToolWindow.ui.jcefSpecificQueries.TaskQueryManager
import org.jetbrains.annotations.TestOnly
import javax.swing.JComponent

class JCEFToolWindow(project: Project) : TaskToolWindow(project) {
  private val taskInfoJBCefBrowser = TaskInfoJBCefBrowser(project)
  private val taskSpecificJBCefBrowser = TaskSpecificJBCefBrowser()
  private var taskSpecificQueryManager: TaskQueryManager<out Task>? = null

  init {
    Disposer.register(this, taskInfoJBCefBrowser)
    Disposer.register(this, taskSpecificJBCefBrowser)

    ApplicationManager.getApplication().messageBus.connect(this)
      .subscribe(LafManagerListener.TOPIC,
                 LafManagerListener { TaskToolWindowView.updateAllTabs(project) })
  }

  override val taskInfoPanel: JComponent
    get() = taskInfoJBCefBrowser.component

  override val taskSpecificPanel: JComponent
    get() = taskSpecificJBCefBrowser.component

  override val uiMode: JavaUILibrary
    get() = JavaUILibrary.JCEF

  override fun setText(text: String) {
    taskInfoJBCefBrowser.loadHTML(text)
  }

  override fun updateTaskSpecificPanel(task: Task?) {
    taskSpecificJBCefBrowser.component.isVisible = false

    val taskText = getHTMLTemplateText(task) ?: return

    // Dispose taskSpecificQueryManager manually because this disposes existing JSQueries and removes them from JS_QUERY_POOL
    taskSpecificQueryManager?.let {
      Disposer.dispose(it)
    }

    taskSpecificQueryManager = getTaskSpecificQueryManager(task, taskSpecificJBCefBrowser)

    taskSpecificJBCefBrowser.component.preferredSize = JBUI.size(Int.MAX_VALUE, 250)
    val html = htmlWithResources(project, taskText, task)
    taskSpecificJBCefBrowser.loadHTML(html)
    taskSpecificJBCefBrowser.component.isVisible = true
  }

  override fun dispose() {
    super.dispose()
    // Dispose undisposed yet taskSpecificQueryManager
    taskSpecificQueryManager?.let {
      Disposer.dispose(it)
    }
  }

  companion object {

    @TestOnly
    fun processContent(content: String, project: Project): String {
      return htmlWithResources(project, content)
    }
  }
}
