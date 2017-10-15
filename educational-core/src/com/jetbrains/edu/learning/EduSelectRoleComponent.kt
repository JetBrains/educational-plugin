package com.jetbrains.edu.learning

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Ref
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.ui.EduSelectRolePanel


class EduSelectRoleComponent : ApplicationComponent {

  override fun getComponentName() = "edu.selectRole"

  override fun disposeComponent() {}

  override fun initComponent() {
    if (PropertiesComponent.getInstance().isValueSet(CCPluginToggleAction.COURSE_CREATOR_ENABLED)) {
      return
    }

    val connection = ApplicationManager.getApplication().messageBus.connect()
    connection.subscribe(AppLifecycleListener.TOPIC, object : AppLifecycleListener {

      override fun appFrameCreated(commandLineArgs: Array<String>?, willOpenProject: Ref<Boolean>) {
        showInitialConfigurationDialog()
      }
    })
  }

  private fun showInitialConfigurationDialog() {
    val dialog = DialogBuilder()
    val panel = EduSelectRolePanel()
    dialog.setPreferredFocusComponent(panel.getStudentButton())
    dialog.title("Are you Student or Teacher?").centerPanel(panel)
    dialog.addOkAction().setText("Start using EduTools")
    dialog.show()
  }
}