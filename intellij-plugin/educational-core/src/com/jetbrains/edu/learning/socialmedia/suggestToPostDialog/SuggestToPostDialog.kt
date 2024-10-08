package com.jetbrains.edu.learning.socialmedia.suggestToPostDialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.socialmedia.SocialMediaSettings
import com.jetbrains.edu.learning.socialmedia.SocialmediaPluginConfigurator
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import java.nio.file.Path
import javax.swing.Box
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Dialog wrapper class with DoNotAsk option for asking user to tweet.
 */
class SuggestToPostDialog(
  project: Project,
  configurators: List<SocialmediaPluginConfigurator>,
  message: String,
  imagePath: Path?,
) : SuggestToPostDialogUI, DialogWrapper(project) {


  private val panel: SuggestToPostDialogPanel
  private val checkBoxes = mutableMapOf<SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState>, JBCheckBox>()

  init {
    title = EduCoreBundle.message("linkedin.dialog.title")

    setDoNotAskOption(SuggestDoNotAskToPostOption(checkBoxes))
    setOKButtonText(EduCoreBundle.message("linkedin.post.button.text"))
    setResizable(false)
    panel = SuggestToPostDialogPanel(message, imagePath, disposable)

    configurators.map { it.settings }.forEach { settings ->
      val checkBox = JBCheckBox(settings.name, settings.askToPost)
      checkBoxes.put(settings, checkBox)
      checkBox.addItemListener {
        isOKActionEnabled = checkBoxes.values.any { it.isSelected }
      }
    }
    if (configurators.size > 1) {
      val checkBoxesPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
      checkBoxesPanel.add(JBLabel(EduCoreBundle.message("label.share.on")))
      checkBoxesPanel.add(Box.createHorizontalStrut(10))
      checkBoxes.forEach {
        checkBoxesPanel.add(it.value)
        checkBoxesPanel.add(Box.createHorizontalStrut(10))
      }
      panel.add(Box.createVerticalStrut(JBUI.scale(10)))
      panel.add(checkBoxesPanel)
    }

    initValidation()
    init()

    myCheckBoxDoNotShowDialog.addItemListener {
      if (it.getStateChange() == ItemEvent.DESELECTED) {
        checkBoxes.forEach { it.value.isEnabled = true }
        isOKActionEnabled = checkBoxes.values.any { it.isSelected }
      }
      if (it.getStateChange() == ItemEvent.SELECTED) {
        checkBoxes.forEach { it.value.isEnabled = false }
        isOKActionEnabled = false
      }
    }
  }

  override fun createCenterPanel(): JComponent = panel
  override fun doValidate(): ValidationInfo? = panel.doValidate()

  private class SuggestDoNotAskToPostOption(val checkBoxes: MutableMap<SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState>, JBCheckBox>) :
    com.intellij.openapi.ui.DoNotAskOption {
    override fun setToBeShown(toBeShown: Boolean, exitCode: Int) {
      if (exitCode == CANCEL_EXIT_CODE || exitCode == OK_EXIT_CODE) {
        if (!toBeShown) {
          checkBoxes.forEach { it.key.askToPost = false }
        }
        else {
          if (exitCode == OK_EXIT_CODE) checkBoxes.forEach { it.key.askToPost = it.value.isSelected }
        }
      }
    }

    override fun isToBeShown(): Boolean = true
    override fun canBeHidden(): Boolean = true
    override fun shouldSaveOptionsOnCancel(): Boolean = true
    override fun getDoNotShowMessage(): String = EduCoreBundle.message("twitter.dialog.do.not.ask")
  }
}
