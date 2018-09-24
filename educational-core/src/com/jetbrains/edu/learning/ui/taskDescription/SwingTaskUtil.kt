@file:JvmName("SwingTaskUtil")

package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.diagnostic.Logger.getInstance
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.refactoring.changeClassSignature.ChangeClassSignatureDialog
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.io.IOException
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit

private const val LEFT_INSET = 15
private const val RIGHT_INSET = 10
private const val TOP_INSET = 15
private const val BOTTOM_INSET = 10

private val LOG = getInstance(ChangeClassSignatureDialog::class.java)

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

fun createTextPane(): JTextPane {
  val textPane = JTextPane()
  textPane.contentType = HTMLEditorKit().contentType
  textPane.editorKit = UIUtil.getHTMLEditorKit(false)
  textPane.isEditable = false
  textPane.background = TaskDescriptionView.getTaskDescriptionBackgroundColor()

  return textPane
}

fun textWithStyles(content: String): String {

  val classLoader = BrowserWindow::class.java.classLoader
  val templateName = when {
    SystemInfo.isMac -> if (UIUtil.isUnderDarcula()) "mac_dark.html" else "mac.html"
    SystemInfo.isLinux -> if (UIUtil.isUnderDarcula()) "win_dark.html" else "win.html"
    else -> if (UIUtil.isUnderDarcula()) "linux_dark.html" else "linux.html"
  }
  var templateText = loadTemplateText(classLoader, templateName)

  if (templateText == null) {
    LOG.warn("Code mirror template is null")
    return content
  }

  val bodyFontSize = bodyFontSize()
  val codeFontSize = codeFontSize()

  val bodyLineHeight = bodyLineHeight()
  val codeLineHeight = codeLineHeight()

  val dimmedColor = ColorUtil.toHex(ColorUtil.dimmer(UIUtil.getPanelBackground()))

  templateText = templateText.replace("\${body_font_size}", bodyFontSize.toString())
  templateText = templateText.replace("\${code_font_size}", codeFontSize.toString())
  templateText = templateText.replace("\${body_line_height}", bodyLineHeight.toString())
  templateText = templateText.replace("\${code_line_height}", codeLineHeight.toString())
  templateText = templateText.replace("\${dimmedColor}", dimmedColor)
  templateText = templateText.replace("\${content}", content)

  return templateText
}

private fun loadTemplateText(classLoader: ClassLoader, templateName: String): String? {
  var templateText: String? = null
  val stream = classLoader.getResourceAsStream("/style/swingTemplates/$templateName")
  try {
    templateText = StreamUtil.readText(stream, "utf-8")
  }
  catch (e: IOException) {
    LOG.warn(e.message)
  }
  finally {
    try {
      stream.close()
    }
    catch (e: IOException) {
      LOG.warn(e.message)
    }

  }
  return templateText
}