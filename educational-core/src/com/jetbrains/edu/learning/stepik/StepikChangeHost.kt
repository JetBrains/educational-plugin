package com.jetbrains.edu.learning.stepik

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EnumComboBoxModel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_URL_PROPERTY
import com.jetbrains.edu.learning.stepik.hyperskill.StepikHost
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


private const val ACTION_TEXT = "Change Stepik url"

@Suppress("ComponentNotRegistered")

class StepikChangeHost : DumbAwareAction(ACTION_TEXT), RightAlignedToolbarAction {

  override fun actionPerformed(e: AnActionEvent) {
    val dialog = StepikChangeHostDialog()
    dialog.show()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isFeatureEnabled(EduExperimentalFeatures.CC_STEPIK)
  }
}

class StepikChangeHostDialog : DialogWrapper(true) {
  private val panel = JPanel()
  private val hostsCombo = ComboBox(EnumComboBoxModel(StepikHost::class.java))
  private val LOG: Logger = Logger.getInstance(StepikChangeHostDialog::class.java)

  init {
    title = "Change Stepik url"
    setOKButtonText("Select")
    panel.preferredSize = JBUI.size(300, 50)
    panel.minimumSize = preferredSize
    panel.add(JLabel("Select Stepik url:"))
    panel.add(hostsCombo)
    val initialValue = StepikHost.getSelectedHost()
    if (initialValue != null) {
      hostsCombo.selectedItem = initialValue
    }
    init()
    UIUtil.setBackgroundRecursively(rootPane, MAIN_BG_COLOR)
  }

  override fun createCenterPanel(): JComponent = panel

  override fun doOKAction() {
    val selectedUrl = (hostsCombo.selectedItem as? StepikHost)?.url

    if (selectedUrl != null) {
      PropertiesComponent.getInstance().setValue(STEPIK_URL_PROPERTY, selectedUrl)
      LOG.info("Stepik url was changed to ${selectedUrl}")
      close(OK_EXIT_CODE)
    }
    else {
      LOG.info("Selected Stepik url item is null")
      return
    }
  }
}