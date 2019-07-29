@file:JvmName("JavaFxTaskUtil")
package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.ide.BrowserUtil
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.ui.taskDescription.styleManagers.StyleManager
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import org.apache.commons.lang.text.StrSubstitutor
import org.jsoup.Jsoup
import java.io.File
import java.util.*

private const val LEFT_INSET = 3.0
private const val RIGHT_INSET = 10.0
private const val TOP_INSET = 15.0
private const val BOTTOM_INSET = 10.0

const val MULTIPLE_CHOICE_LABEL = "Select one or more options from the list:"
const val SINGLE_CHOICE_LABEL = "Select one option from the list:"

fun Task.createScene(): Scene? {
  val choiceTask = this as? ChoiceTask ?: return null
  return choiceTask.createScene()
}

fun ChoiceTask.createScene(): Scene {
  val group = Group()
  val scene = Scene(group, getSceneBackground())

  val vBox = VBox()
  vBox.spacing = JBUI.scale(10).toDouble()
  vBox.padding = Insets(TOP_INSET, RIGHT_INSET, BOTTOM_INSET, LEFT_INSET)
  if (this.isMultipleChoice) {
    val text = createLabel(MULTIPLE_CHOICE_LABEL)

    vBox.children.add(text)
    for ((index, option) in this.choiceOptions.withIndex()) {
      val checkBox = createCheckbox(option.text, index, this)
      vBox.children.add(checkBox)
    }
  }
  else {
    val toggleGroup = ToggleGroup()
    val text = createLabel(SINGLE_CHOICE_LABEL)
    vBox.children.add(text)
    for ((index, option) in this.choiceOptions.withIndex()) {
      val radioButton = createRadioButton(option.text, index, toggleGroup, this)
      vBox.children.add(radioButton)
    }
  }
  group.children.add(vBox)

  LafManager.getInstance().addLafManagerListener(StudyLafManagerListener(scene))
  return scene
}

private fun createSelectionListener(task: ChoiceTask, index: Int): (ObservableValue<out Boolean>, Boolean, Boolean) -> Unit {
  return { _, _, isSelected ->
    if (isSelected) {
      task.selectedVariants.add(index)
    }
    else {
      task.selectedVariants.remove(index)
    }
  }
}

private fun createLabel(text: String): Label {
  val textLabel = Label(text)
  setUpLabelStyle(textLabel)
  return textLabel
}

private fun createCheckbox(variant: String, index: Int, task: ChoiceTask): CheckBox {
  val checkBox = CheckBox(variant)
  checkBox.isMnemonicParsing = false
  checkBox.isSelected = task.selectedVariants.contains(index)
  checkBox.selectedProperty().addListener(createSelectionListener(task, index))
  setUpButtonStyle(checkBox)
  return checkBox
}

private fun createRadioButton(variant: String, index: Int, toggleGroup: ToggleGroup, task: ChoiceTask): RadioButton {
  val isSelected = task.selectedVariants.contains(index)
  val radioButton = RadioButton(variant)
  radioButton.toggleGroup = toggleGroup
  radioButton.isSelected = isSelected
  radioButton.selectedProperty().addListener(createSelectionListener(task, index))
  setUpButtonStyle(radioButton)
  return radioButton
}

private fun getSceneBackground(): Color {
  val isDarcula = LafManager.getInstance().currentLookAndFeel is DarculaLookAndFeelInfo
  val panelBackground = if (isDarcula) UIUtil.getPanelBackground() else UIUtil.getTextFieldBackground()
  return Color.rgb(panelBackground.red, panelBackground.green, panelBackground.blue)
}

private fun setUpLabelStyle(node: Label) {
  node.stylesheets.add(StyleManager().baseStylesheet)
  node.font = Font(StyleManager().bodyFont, getFontSize())
  val labelForeground = UIUtil.getLabelForeground()
  node.textFill = Color.rgb(labelForeground.red, labelForeground.green, labelForeground.blue)
}

private fun getFontSize() = (EditorColorsManager.getInstance().globalScheme.editorFontSize + 1).toDouble()

private fun setUpButtonStyle(button: ButtonBase) {
  button.isWrapText = true
  button.font = Font.font(getFontSize())
  setButtonLaf(button)
}

fun Scene.updateLaf() {
  Platform.runLater {
    val panelBackground = UIUtil.getPanelBackground()
    val root = this.root
    this.fill = Color.rgb(panelBackground.red, panelBackground.green, panelBackground.blue)
    for (node in getAllNodes(root)) {
      (node as? ButtonBase)?.let { setButtonLaf(it) }
      (node as? Label)?.let { setUpLabelStyle(it) }
    }
  }
}

fun getAllNodes(root: Parent): ArrayList<Node> {
  val nodes = ArrayList<Node>()
  addAllDescendants(root, nodes)
  return nodes
}

private fun addAllDescendants(parent: Parent, nodes: ArrayList<Node>) {
  for (node in parent.childrenUnmodifiable) {
    nodes.add(node)
    (node as? Parent)?.let { addAllDescendants(it, nodes) }
  }
}

fun setButtonLaf(button: ButtonBase) {
  button.stylesheets.removeAll()
  button.stylesheets.addAll(StyleManager().buttonStylesheets)
}

fun htmlWithResources(project: Project, content: String): String {
  val templateText = loadText("/style/template.html.ft")
  val styleManager = StyleManager()

  val textWithResources = StrSubstitutor(styleManager.resources(content)).replace(templateText) ?: "Cannot load task text"
  return absolutizeImgPaths(project, textWithResources)
}

fun loadText(filePath: String): String? {
  val stream = object {}.javaClass.getResourceAsStream(filePath)
  stream.use {
    return StreamUtil.readText(stream, "utf-8")
  }
}

private fun absolutizeImgPaths(project: Project, content: String): String {
  val srcAttribute = "src"
  val task = EduUtils.getCurrentTask(project)
  if (task == null) {
    return content
  }

  val taskDir = task.getTaskDir(project)
  if (taskDir == null) {
    return content
  }

  val document = Jsoup.parse(content)
  val imageElements = document.getElementsByTag("img")
  for (imageElement in imageElements) {
    val imagePath = imageElement.attr(srcAttribute)
    if (!BrowserUtil.isAbsoluteURL(imagePath)) {
      val file = File(imagePath)
      val absolutePath = File(taskDir.path, file.path).toURI().toString()
      imageElement.attr("src", absolutePath)
    }
  }
  return document.outerHtml()
}

private class StudyLafManagerListener(val scene: Scene) : LafManagerListener {
  override fun lookAndFeelChanged(manager: LafManager) {
    scene.updateLaf()
  }
}
