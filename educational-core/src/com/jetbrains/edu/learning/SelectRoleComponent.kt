package com.jetbrains.edu.learning

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.coroutineDispatchingContext
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.ui.SelectRolePanel
import kotlinx.coroutines.runBlocking

@Suppress("ComponentNotRegistered") // educational-core.xml
class SelectRoleComponent : BaseComponent {
  override fun getComponentName() = "edu.selectRole"

  override fun disposeComponent() {}

  override fun initComponent() {
    if (!PlatformUtils.isPyCharmEducational() && PlatformUtils.getPlatformPrefix() != PlatformUtils.IDEA_EDU_PREFIX) {
      PropertiesComponent.getInstance().setValue(CCPluginToggleAction.COURSE_CREATOR_ENABLED, true)
      return
    }

    if (PropertiesComponent.getInstance().isValueSet(CCPluginToggleAction.COURSE_CREATOR_ENABLED)) {
      return
    }

    val connection = ApplicationManager.getApplication().messageBus.connect()
    connection.subscribe(AppLifecycleListener.TOPIC, object : AppLifecycleListener {
      override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        // HACK: ActionManager is instantiated here
        // otherwise it is instantiated during dialog showing (to render buttons on Mac OS magic touch bar)
        // which causes assert because one shouldn't instantiate ActionManager in EDT
        ActionManager.getInstance()

        runBlocking(AppUIExecutor.onUiThread().coroutineDispatchingContext()) {
          showInitialConfigurationDialog()
        }
      }
    })
  }

  private fun showInitialConfigurationDialog() {
    val dialog = DialogBuilder()
    val panel = SelectRolePanel()
    dialog.setPreferredFocusComponent(panel.getStudentButton())
    dialog.title("Are you a Learner or an Educator?").centerPanel(panel)
    dialog.addOkAction().setText("Start using EduTools")
    dialog.show()
  }
}