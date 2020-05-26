package com.jetbrains.edu.learning.twitter

import com.intellij.openapi.project.Project
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.*
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OptionsProvider
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class TwitterOptionsProvider(private val project: Project) : OptionsProvider {

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
    // Default project can be passed here while building searchable options
    if (project.isDefault) return null
    if (!EduUtils.isEduProject(project)) return null
    if (EduUtils.getTwitterConfigurator(project) == null) return null

    return panel {
      row { askToTweetCheckBox() }
    }.apply {
      border = IdeBorderFactory.createTitledBorder(EduCoreBundle.message("twitter.configurable.name"))
    }
  }
}
