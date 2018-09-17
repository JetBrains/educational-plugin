@file:JvmName("JavaFxTaskSpecificPanel")
package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.ui.taskDescription.BrowserWindow.getBrowserStylesheet
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
import java.util.*

private const val LEFT_INSET = 15.0
private const val RIGHT_INSET = 10.0
private const val TOP_INSET = 15.0
private const val BOTTOM_INSET = 10.0

const val MULTIPLE_CHOICE_LABEL = "Select one or more options from the list:"
const val SINGLE_CHOICE_LABEL = "Select one option from the list:"

fun Task?.createScene(): Scene? {
  val choiceTask = this as? ChoiceTask ?: return null
  return choiceTask.createScene()
}

fun ChoiceTask.createScene(): Scene {
  val group = Group()
  val scene = Scene(group, getSceneBackground())

  Platform.runLater {
    val vBox = VBox()
    vBox.spacing = 10.0
    vBox.padding = Insets(TOP_INSET, RIGHT_INSET, BOTTOM_INSET, LEFT_INSET)
    if (this.isMultipleChoice) {
      val text = createLabel(MULTIPLE_CHOICE_LABEL)

      vBox.children.add(text)
      for ((index, variant) in this.choiceVariants.withIndex()) {
        val checkBox = createCheckbox(variant, index, this)
        vBox.children.add(checkBox)
      }
    }
    else {
      val toggleGroup = ToggleGroup()
      val text = createLabel(SINGLE_CHOICE_LABEL)
      vBox.children.add(text)
      for ((index, variant) in this.choiceVariants.withIndex()) {
        val radioButton = createRadioButton(variant, index, toggleGroup, this)
        vBox.children.add(radioButton)
      }
    }
    group.children.add(vBox)
  }

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
  val isDarcula = LafManager.getInstance().currentLookAndFeel is DarculaLookAndFeelInfo
  val engineStyleUrl = object {}.javaClass.getResource(getBrowserStylesheet(isDarcula))
  node.stylesheets.add(engineStyleUrl.toExternalForm())
  node.font = Font.font((EditorColorsManager.getInstance().globalScheme.editorFontSize + 2).toDouble())
}

private fun setUpButtonStyle(button: ButtonBase) {
  button.isWrapText = true
  button.font = Font.font((EditorColorsManager.getInstance().globalScheme.editorFontSize + 2).toDouble())
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
  val darcula = LafManager.getInstance().currentLookAndFeel is DarculaLookAndFeelInfo
  val stylesheetPath = if (darcula) "/style/buttonsDarcula.css" else "/style/buttons.css"
  button.stylesheets.removeAll()
  button.stylesheets.add(object {}.javaClass.getResource(stylesheetPath).toExternalForm())
  val engineStyleUrl = object {}.javaClass.getResource(getBrowserStylesheet(darcula))
  button.stylesheets.add(engineStyleUrl.toExternalForm())
}

private class StudyLafManagerListener(val scene: Scene) : LafManagerListener {
  override fun lookAndFeelChanged(manager: LafManager) {
    scene.updateLaf()
  }
}
