package com.jetbrains.edu.learning.twitter

import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.*
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OptionsProvider
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class TwitterOptionsProvider : OptionsProvider {

  private val askToTweetCheckBox: JBCheckBox = JBCheckBox(
    EduCoreBundle.message("twitter.ask.to.tweet"),
    TwitterSettings.getInstance().askToTweet()
  )

  @Nls
  override fun getDisplayName(): String = EduCoreBundle.message("twitter.configurable.name")

  override fun isModified(): Boolean {
    return TwitterSettings.getInstance().askToTweet() != askToTweetCheckBox.isSelected
  }

  override fun apply() {
    TwitterSettings.getInstance().setAskToTweet(askToTweetCheckBox.isSelected)
  }

  override fun reset() {
    askToTweetCheckBox.isSelected = TwitterSettings.getInstance().askToTweet()
  }

  override fun createComponent(): JComponent? {
    return panel {
      row { askToTweetCheckBox() }
    }.apply {
      border = IdeBorderFactory.createTitledBorder(EduCoreBundle.message("twitter.configurable.name"))
    }
  }
}
