package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.SeparatorComponent
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.actions.LeaveFeedbackAction
import com.jetbrains.edu.learning.actions.RefreshTaskFileAction
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel


class TaskDescriptionPanel : SimpleToolWindowPanel(true, true), DataProvider, Disposable {
  override fun dispose() {

  }

  val icon = object: AsyncProcessIcon("check") {
    override fun setVisible(aFlag: Boolean) {
      super.setVisible(aFlag)
    }
  }

  val middlePanel: JPanel = JPanel(BorderLayout())

  init {
    val defaultBackground = EditorColorsManager.getInstance().globalScheme.defaultBackground
    icon.isVisible = false
    val nextButton = JButton("Next")
    nextButton.background = defaultBackground
    nextButton.isVisible = false

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
    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)


    val jbScrollPane = JBScrollPane(label)
    panel.add(jbScrollPane)
    jbScrollPane.alignmentX = Component.LEFT_ALIGNMENT

    val jSeparator = SeparatorComponent()
    jSeparator.alignmentX = Component.LEFT_ALIGNMENT
    panel.add(jSeparator)

    val bottomPanel = JPanel(BorderLayout())


    val action = ActionManager.getInstance().getAction(CheckAction.ACTION_ID)

    val toolbar = ActionManager.getInstance().createButtonToolbar(ActionPlaces.UNKNOWN, DefaultActionGroup(action))


    val checkPanel = JPanel(BorderLayout())
    checkPanel.add(toolbar, BorderLayout.WEST)


    setDefaultStateForMiddlePanel()
    middlePanel.border = JBUI.Borders.empty(0, 16, 0, 0)
    checkPanel.add(middlePanel, BorderLayout.CENTER)

    val commentAction = ActionManager.getInstance().getAction(LeaveFeedbackAction.ACTION_ID)
    val refreshAction = ActionManager.getInstance().getAction(RefreshTaskFileAction.ACTION_ID)
    val toolbar1 = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, DefaultActionGroup(
       commentAction), true)
    toolbar1.layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY
    toolbar1.adjustTheSameSize(true)

    val toolbar2 = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, DefaultActionGroup(
      refreshAction), true)
    toolbar2.layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY
    toolbar2.adjustTheSameSize(true)
    val component2 = toolbar2.component
    component2.border = JBUI.Borders.empty(0, 0, 0, 0)

    val component1 = toolbar1.component
    component1.border = JBUI.Borders.empty(0, 0, 0, 0)

    val actionsPanel = JPanel(HorizontalLayout(0))
    actionsPanel.add(component2)
    actionsPanel.add(component1)
    checkPanel.add(actionsPanel, BorderLayout.EAST)

    bottomPanel.add(checkPanel, BorderLayout.NORTH)
    bottomPanel.alignmentX = Component.LEFT_ALIGNMENT

    panel.add(bottomPanel)

    UIUtil.setBackgroundRecursively(panel, defaultBackground)

    setContent(panel)
  }

  fun setDefaultStateForMiddlePanel() {
    middlePanel.removeAll()
    middlePanel.add(icon, BorderLayout.WEST)
    middlePanel.add(JPanel(), BorderLayout.CENTER)
    UIUtil.setBackgroundRecursively(middlePanel, EditorColorsManager.getInstance().globalScheme.defaultBackground)
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