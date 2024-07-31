package com.jetbrains.edu.learning.socialmedia.suggestToPostDialog

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.socialmedia.SocialMediaSettings
import javax.swing.JComponent

/**
 * Dialog wrapper class with DoNotAsk option for asking user to tweet.
 */
abstract class SuggestToPostDialog(
  project: Project,
  dialogPanelCreator: (Disposable) -> SuggestToPostDialogPanel,
  dialogTitle: String,
  okButtonText: String,
  val settings: SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState>
) : DialogWrapper(project), SuggestToPostDialogUI {


  private val panel: SuggestToPostDialogPanel

  init {
    title = dialogTitle
    setDoNotAskOption(SuggestDoNotAskToPostOption(settings))
    setOKButtonText(okButtonText)
    setResizable(false)
    panel = dialogPanelCreator(disposable)

    initValidation()
    init()
  }

  override val message: String get() = panel.message

  override fun createCenterPanel(): JComponent = panel
  override fun doValidate(): ValidationInfo? = panel.doValidate()

  private class SuggestDoNotAskToPostOption(val settings: SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState>) :
    com.intellij.openapi.ui.DoNotAskOption {
    override fun setToBeShown(toBeShown: Boolean, exitCode: Int) {
      if (exitCode == CANCEL_EXIT_CODE || exitCode == OK_EXIT_CODE) {
        settings.askToPost = toBeShown
      }
    }

    override fun isToBeShown(): Boolean = true
    override fun canBeHidden(): Boolean = true
    override fun shouldSaveOptionsOnCancel(): Boolean = true
    override fun getDoNotShowMessage(): String = EduCoreBundle.message("twitter.dialog.do.not.ask")
  }
}
