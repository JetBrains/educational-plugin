package com.jetbrains.edu.learning.twitter.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.twitter.TwitterSettings
import javax.swing.JComponent

/**
 * Dialog wrapper class with DoNotAsk option for asking user to tweet.
 */
class TwitterDialog(
  project: Project,
  private val panel: TwitterDialogPanel
) : DialogWrapper(project) {

  init {
    title = EduCoreBundle.message("twitter.dialog.title")
    setDoNotAskOption(DoNotAskOption())
    setOKButtonText(EduCoreBundle.message("twitter.dialog.ok.action"))
    setResizable(true)
    val preferredSize = panel.preferredSize
    setSize(preferredSize.getHeight().toInt(), preferredSize.getWidth().toInt())

    initValidation()
    init()
  }

  override fun createCenterPanel(): JComponent? = panel
  override fun doValidate(): ValidationInfo? = panel.doValidate()

  private class DoNotAskOption : DialogWrapper.DoNotAskOption {
    override fun setToBeShown(toBeShown: Boolean, exitCode: Int) {
      if (exitCode == CANCEL_EXIT_CODE || exitCode == OK_EXIT_CODE) {
        TwitterSettings.getInstance().setAskToTweet(toBeShown)
      }
    }
    override fun isToBeShown(): Boolean = true
    override fun canBeHidden(): Boolean = true
    override fun shouldSaveOptionsOnCancel(): Boolean = true
    override fun getDoNotShowMessage(): String = EduCoreBundle.message("twitter.dialog.do.not.ask")
  }
}
