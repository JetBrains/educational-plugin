@file:JvmName("SwingTaskUtil")

package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingBasedTask
import com.jetbrains.edu.learning.taskToolWindow.ui.specificTaskSwingPanels.ChoiceTaskSpecificPanel
import com.jetbrains.edu.learning.taskToolWindow.ui.specificTaskSwingPanels.SortingBasedTaskSpecificPanel
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.border.Border
import javax.swing.text.html.HTMLEditorKit

fun createSpecificPanel(task: Task?): JPanel? {
  return when (task) {
    is ChoiceTask -> ChoiceTaskSpecificPanel(task)
    is SortingBasedTask -> SortingBasedTaskSpecificPanel(task)
    else -> null
  }
}

fun createTextPane(editorKit: HTMLEditorKit = HTMLEditorKitBuilder().withWordWrapViewFactory().build()): JTextPane {
  prepareCss(editorKit)

  val textPane = object : JTextPane() {
    override fun getSelectedText(): String {
      // see EDU-3185
      return super.getSelectedText().replace(Typography.nbsp, ' ')
    }
  }

  textPane.contentType = editorKit.contentType
  textPane.editorKit = editorKit
  textPane.isEditable = false
  textPane.background = TaskToolWindowView.getTaskDescriptionBackgroundColor()

  return textPane
}

private fun prepareCss(editorKit: HTMLEditorKit) {
  // ul padding of JBHtmlEditorKit is too small, so copy-pasted the style from
  // com.intellij.codeInsight.documentation.DocumentationComponent.prepareCSS
  editorKit.styleSheet.addRule("ul { padding: 3px 16px 0 0; }")
  editorKit.styleSheet.addRule("li { padding: 3px 0 4px 5px; }")
  editorKit.styleSheet.addRule(".hint { padding: 17px 0 16px 0; }")
}

const val HINT_PROTOCOL = "hint://"

fun JPanel.addBorder(newBorder: Border?): JPanel {
  return apply {
    border = JBUI.Borders.compound(newBorder, border)
  }
}
