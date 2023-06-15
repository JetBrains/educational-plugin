package com.jetbrains.edu.learning.stepik.changeHost

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.dsl.builder.bindItemNullable
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_HOST_ORDINAL_PROPERTY
import org.jetbrains.annotations.NonNls
import javax.swing.JComponent

class StepikChangeHost : DumbAwareAction(EduCoreBundle.message("stepik.change.host")) {

  override fun actionPerformed(e: AnActionEvent) {
    val selectedHost = showStepikChangeHostDialog()

    if (selectedHost != null) {
      PropertiesComponent.getInstance().setValue(STEPIK_HOST_ORDINAL_PROPERTY, selectedHost.ordinal, StepikHost.PRODUCTION.ordinal)
      EduSettings.getInstance().user = null
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
  var selectedHost: StepikHost? = StepikHost.getSelectedHost()
    private set

  init {
    title = EduCoreBundle.message("stepik.change.host")
    setOKButtonText(EduCoreBundle.message("stepik.select"))
    isResizable = false
    init()
  }

  override fun createCenterPanel(): JComponent = panel {
    row(EduCoreBundle.message("stepik.url")) {
      @Suppress("UnstableApiUsage", "DEPRECATION")
      comboBox(EnumComboBoxModel(StepikHost::class.java))
        // BACKCOMPAT: 2022.3. Use bindItem instead
        .bindItemNullable(::selectedHost)
        .focused()
    }
  }
}