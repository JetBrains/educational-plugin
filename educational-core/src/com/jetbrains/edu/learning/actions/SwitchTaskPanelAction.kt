package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionPlaces.ACTION_SEARCH
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.JavaUILibrary.*
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindowFactory
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JLabel


@Suppress("ComponentNotRegistered") // registered in educational-core.xml
class SwitchTaskPanelAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    val result = createDialog().showAndGet()
    if (result && project != null) {
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
      toolWindow.contentManager.removeAllContents(false)
      TaskDescriptionToolWindowFactory().createToolWindowContent(project, toolWindow)
      TaskDescriptionView.getInstance(project).updateTaskDescription()
    }
  }

  private fun createDialog(): DialogWrapper = MyDialog(false)

  class MyDialog(canBeParent: Boolean) : DialogWrapper(null, canBeParent) {
    private val myComboBox: ComboBox<JavaUILibrary> = ComboBox()

    override fun createCenterPanel(): JComponent? = myComboBox

    override fun createNorthPanel(): JComponent? = JLabel("Choose panel: ")

    override fun getPreferredFocusedComponent(): JComponent? = myComboBox

    override fun doOKAction() {
      super.doOKAction()
      val selectedUILibrary = myComboBox.selectedItem as JavaUILibrary?
      EduSettings.getInstance().javaUiLibrary = selectedUILibrary ?: SWING
    }

    init {
      val comboBoxModel = DefaultComboBoxModel<JavaUILibrary>()
      comboBoxModel.addElement(SWING)
      if (EduUtils.hasJavaFx()) {
        comboBoxModel.addElement(JAVAFX)
      }
      if (EduUtils.hasJCEF()) {
        comboBoxModel.addElement(JCEF)
      }

      comboBoxModel.selectedItem = EduSettings.getInstance().javaUiLibraryWithCheck
      myComboBox.model = comboBoxModel
      title = "Switch Task Description Panel"
      myComboBox.setMinimumAndPreferredWidth(250)
      init()
    }
  }

  override fun update(e: AnActionEvent) {
    val place = e.place
    val project = e.project
    e.presentation.isEnabled = project != null && EduUtils.isEduProject(project) || ACTION_SEARCH == place
  }
}