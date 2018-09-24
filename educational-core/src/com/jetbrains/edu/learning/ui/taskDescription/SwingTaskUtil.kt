@file:JvmName("SwingTaskUtil")

package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.awt.Font
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit

private const val LEFT_INSET = 15
private const val RIGHT_INSET = 10
private const val TOP_INSET = 15
private const val BOTTOM_INSET = 10


fun Task?.createSpecificPanel(): JPanel? {
  val choiceTask = this as? ChoiceTask ?: return null
  return choiceTask.createSpecificPanel()
}

fun ChoiceTask.createSpecificPanel(): JPanel {
  val jPanel = JPanel(VerticalFlowLayout())
  jPanel.border = JBUI.Borders.empty(TOP_INSET, LEFT_INSET, BOTTOM_INSET, RIGHT_INSET)

  if (this.isMultipleChoice) {
    val text = JLabel(MULTIPLE_CHOICE_LABEL, SwingConstants.LEFT)
    jPanel.add(text)

    for ((index, variant) in this.choiceVariants.withIndex()) {
      val checkBox = createCheckBox(variant, index, this)
      jPanel.add(checkBox)
    }
  }
  else {
    val text = JLabel(SINGLE_CHOICE_LABEL, SwingConstants.LEFT)
    jPanel.add(text)

    val group = ButtonGroup()
    for ((index, variant) in this.choiceVariants.withIndex()) {
      val checkBox = createRadioButton(variant, index, group, this)
      jPanel.add(checkBox)
    }
  }

  return jPanel
}

fun createCheckBox(variant: String?, index: Int, task: ChoiceTask): JCheckBox {
  val checkBox = JCheckBox(variant)
  checkBox.isSelected = task.selectedVariants.contains(index)
  checkBox.addItemListener(createListener(task, index))
  return checkBox
}

fun createRadioButton(variant: String, index: Int, group: ButtonGroup, task: ChoiceTask): JRadioButton {
  val button = JRadioButton(variant)
  button.isSelected = task.selectedVariants.contains(index)
  button.addItemListener(createListener(task, index))
  group.add(button)
  return button
}

fun createListener(task: ChoiceTask, index: Int): ItemListener? {
  return ItemListener {
    if (it.stateChange == ItemEvent.SELECTED) {
      task.selectedVariants.add(index)
    }
    else {
      task.selectedVariants.remove(index)
    }
  }
}

fun createTextPaneWithStyleSheet(): JTextPane {
  val textPane = JTextPane()
  textPane.contentType = HTMLEditorKit().contentType

  val editorColorsScheme = EditorColorsManager.getInstance().globalScheme
  val font = Font(editorColorsScheme.editorFontName, Font.PLAIN, editorColorsScheme.editorFontSize)
  val dimmedColor = ColorUtil.toHex(ColorUtil.dimmer(UIUtil.getPanelBackground()))
  val fontSize = font.size

  val bodyRule = "body { font-family: ${font.family}; font-size: ${fontSize}pt; }"
  val preRule = """
    pre {
      font-family: Courier;
      font-size: $fontSize;
      display: inline;
      line-height: 50px;
      padding-top: 5px;
      padding-bottom: 5px;
      padding-left: 5px;
      background-color:$dimmedColor;
    }
  """.trimIndent()
  val codeRule = "code {font-family: Courier; font-size:${fontSize}pt; display: flex; float: left; background-color: $dimmedColor;}"
  val headersRule = "h1 { font-size: ${2 * fontSize}pt; } h2 { font-size: ${1.5 * fontSize}pt; }"

  val htmlEditorKit = UIUtil.getHTMLEditorKit(false)
  val styleSheet = htmlEditorKit.styleSheet

  for (rule in listOf(bodyRule, preRule, codeRule, headersRule)) {
    styleSheet.addRule(rule)
  }

  textPane.editorKit = htmlEditorKit
  textPane.isEditable = false

  textPane.background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
  return textPane
}
