package com.jetbrains.edu.kotlin.twitter

import com.intellij.openapi.project.Project
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.*
import com.jetbrains.edu.kotlin.messages.EduKtBundle
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.settings.OptionsProvider
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class KtOptionsProvider(private val project: Project) : OptionsProvider {

  private val askToTweetCheckBox: JBCheckBox = JBCheckBox(
    EduKtBundle.message("ktoptions.ask.to.tweet"),
    KtTwitterSettings.getInstance(project).askToTweet()
  )

  @Nls
  override fun getDisplayName(): String = "Twitter Settings"

  override fun isModified(): Boolean {
    return KtTwitterSettings.getInstance(project).askToTweet() != askToTweetCheckBox.isSelected
  }

  override fun apply() {
    KtTwitterSettings.getInstance(project).setAskToTweet(askToTweetCheckBox.isSelected)
  }

  override fun reset() {
    askToTweetCheckBox.isSelected = KtTwitterSettings.getInstance(project).askToTweet()
  }

  override fun createComponent(): JComponent? {
    if (!EduUtils.isEduProject(project)) return null
    if (EduUtils.getTwitterConfigurator(project) == null) return null

    return panel {
      row { askToTweetCheckBox() }
    }.apply {
      border = IdeBorderFactory.createTitledBorder("Kotlin")
    }
  }
}
