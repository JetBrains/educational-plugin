package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionPlaces.ACTION_SEARCH
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.jcef.JBCefApp
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.JavaUILibrary.JCEF
import com.jetbrains.edu.learning.JavaUILibrary.SWING
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowFactory
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.annotations.NonNls
import javax.swing.JComponent

class SwitchTaskPanelAction : DumbAwareAction(EduCoreBundle.lazyMessage("action.switch.task.description.text")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    val result = MyDialog(project).showAndGet()
    if (result && project != null) {
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
      toolWindow.contentManager.removeAllContents(true)
      TaskToolWindowFactory().createToolWindowContent(project, toolWindow)
      TaskToolWindowView.getInstance(project).updateNavigationPanel()
      TaskToolWindowView.getInstance(project).updateTaskDescription()
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    val place = e.place
    val project = e.project
    e.presentation.isEnabled = project != null && project.isEduProject() || ACTION_SEARCH == place
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.SwitchTaskDescriptionPanel"
  }

  private class MyDialog(project: Project?) : DialogWrapper(project, false) {
    init {
      title = EduCoreBundle.message("dialog.title.switch.task.description.panel")
      init()
    }

    override fun createCenterPanel(): JComponent = panel {
      row(EduCoreBundle.message("ui.label.choose.panel")) {
        comboBox(collectAvailableJavaUiLibraries())
          .columns(COLUMNS_SHORT)
          .resizableColumn()
          .align(AlignX.FILL)
          .focused()
          .bindItem(
            getter = { EduSettings.getInstance().javaUiLibrary },
            setter = { EduSettings.getInstance().setJavaUiLibrary(it ?: SWING, changedByUser = true) }
          )
      }
    }

    private fun collectAvailableJavaUiLibraries(): List<JavaUILibrary> {
      val availableJavaUiLibraries = mutableListOf(SWING)
      if (JBCefApp.isSupported()) {
        availableJavaUiLibraries += JCEF
      }
      return availableJavaUiLibraries
    }
  }
}
