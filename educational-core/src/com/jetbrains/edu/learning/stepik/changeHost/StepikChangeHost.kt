package com.jetbrains.edu.learning.stepik.changeHost

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.layout.*
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_HOST_ORDINAL_PROPERTY
import org.jetbrains.annotations.NonNls
import javax.swing.JComponent

@Suppress("ComponentNotRegistered")
class StepikChangeHost : DumbAwareAction(EduCoreBundle.message("stepik.change.host")) {

  override fun actionPerformed(e: AnActionEvent) {
    val selectedHost = showStepikChangeHostDialog()

    if (selectedHost != null) {
      PropertiesComponent.getInstance().setValue(STEPIK_HOST_ORDINAL_PROPERTY, selectedHost.ordinal, StepikHost.PRODUCTION.ordinal)
      LOG.info("Stepik url was changed to ${selectedHost.url}")
    }
    else {
      LOG.info("Selected Stepik url item is null")
    }
  }

  companion object {
    private val LOG: Logger = logger<StepikChangeHost>()

    @NonNls
    const val ACTION_ID = "Educational.Educator.StepikChangeHost"
  }
}

class StepikChangeHostDialog : DialogWrapper(true) {
  private val hostsCombo = ComboBox(EnumComboBoxModel(StepikHost::class.java))

  init {
    title = EduCoreBundle.message("stepik.change.host")
    setOKButtonText(EduCoreBundle.message("stepik.select"))
    hostsCombo.selectedItem = StepikHost.getSelectedHost()
    init()
  }

  override fun createCenterPanel(): JComponent = panel {
    row(EduCoreBundle.message("stepik.url")) { hostsCombo() }
  }

  override fun getPreferredFocusedComponent(): JComponent = hostsCombo

  fun getSelectedItem(): StepikHost? = hostsCombo.selectedItem as StepikHost?
}