package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.SeparatorComponent
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.*
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.actions.CheckAction
import javax.swing.JButton


class TaskDescriptionPanel : SimpleToolWindowPanel(true, true), DataProvider, Disposable {
  override fun dispose() {

  }

  val icon = object: AsyncProcessIcon("check") {
    override fun setVisible(aFlag: Boolean) {
      super.setVisible(aFlag)
    }
  }

  init {
    val defaultBackground = EditorColorsManager.getInstance().globalScheme.defaultBackground
    icon.isVisible = false
    val nextButton = JButton("Next")
    nextButton.background = defaultBackground
    nextButton.isVisible = false

    val action = ActionManager.getInstance().getAction(CheckAction.ACTION_ID)
//    val toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, DefaultActionGroup(action), true)

    val toolbar = ActionManager.getInstance().createButtonToolbar(ActionPlaces.UNKNOWN, DefaultActionGroup(action))
    val panel = panel {
      row {
        val label = JBLabel("<html>sample text<br><br><br>" +
                            "sample text<br><br><br>\n" +
                            "sample text\n" +
                            "sample text<br><br><br>\n" +
                            "sample text\n <br><br><br>" +
                            "<br><br><br>" +
                            "<br><br><br>" +
                            "<br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br>" +
                            "" +
                            "" +
                            "" +
                            "" +
                            "" +
                            "" +
                            "sample text\n" +
                            "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" +
                            "sample text<br><br><br>\n" +
                            "sample text\n" +
                            "sample text\n" + "sample text\n" + "sample text\n" +
                            "sample text\n" +
                            "sample text\n" + "sample text\n</html>")
        label.background = defaultBackground
        scrollPane(label)
      }
      row {
        SeparatorComponent()()
      }
      row {
        cell(isVerticalFlow = false) {
          toolbar()
          icon()
          nextButton()
        }
      }
    }
    UIUtil.setBackgroundRecursively(panel, defaultBackground)
    panel.border = JBUI.Borders.empty(20, 20, 0, 10)
    setContent(panel)
  }

  companion object {
    fun getToolWindow(project: Project): TaskDescriptionPanel? {
      if (project.isDisposed) return null

      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW)
      if (toolWindow != null) {
        val contents = toolWindow.contentManager.contents
        for (content in contents) {
          val component = content.component
          if (component is TaskDescriptionPanel) {
            return component
          }
        }
      }
      return null
    }
  }
}