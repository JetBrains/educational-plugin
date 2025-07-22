package com.jetbrains.edu.socialMedia.suggestToPostDialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.socialMedia.SocialMediaPluginConfigurator
import com.jetbrains.edu.socialMedia.SocialMediaSettings
import com.jetbrains.edu.socialMedia.messages.EduSocialMediaBundle
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
  configurators: List<SocialMediaPluginConfigurator>,
  message: String,
  imagePath: Path?,
) : SuggestToPostDialogUI, DialogWrapper(project) {


  private val panel: SuggestToPostDialogPanel
  private val checkBoxes = mutableMapOf<SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState>, JBCheckBox>()

  init {
    val onlyOneConfigurator = configurators.size == 1
    title = if (onlyOneConfigurator) {
      EduSocialMediaBundle.message("social.media.post.your.achievements.to.dialog.title", configurators[0].settings.name)
    }
    else {
      EduSocialMediaBundle.message("social.media.post.your.achievements.to.social.networks.dialog.title")
    }

    setDoNotAskOption(SuggestDoNotAskToPostOption(checkBoxes))
    setOKButtonText(EduSocialMediaBundle.message("social.media.post.button.text"))
    isResizable = false
    panel = SuggestToPostDialogPanel(message, imagePath, disposable)

    configurators.map { it.settings }.forEach { settings ->
      val checkBox = JBCheckBox(settings.name, settings.askToPost)
      checkBoxes.put(settings, checkBox)
      checkBox.addItemListener {
        isOKActionEnabled = checkBoxes.values.any { it.isSelected }
      }
    }
    if (!onlyOneConfigurator) {
      val checkBoxesPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
      checkBoxesPanel.add(JBLabel(EduSocialMediaBundle.message("social.media.share.on.label")))
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
    override fun getDoNotShowMessage(): String = EduSocialMediaBundle.message("social.media.do.not.ask.dialog.checkbox")
  }
}
