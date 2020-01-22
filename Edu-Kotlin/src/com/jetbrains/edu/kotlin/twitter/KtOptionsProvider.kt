package com.jetbrains.edu.kotlin.twitter

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.settings.OptionsProvider
import org.jetbrains.annotations.Nls
import java.awt.event.ActionEvent
import javax.swing.JComponent
import javax.swing.JPanel

class KtOptionsProvider internal constructor(private val myProject: Project) : OptionsProvider {
  private val myTwitterSettings: KtTwitterSettings
  private val myAskToTweetCheckBox: JBCheckBox? = null
  private val myPanel: JPanel? = null
  private var myIsModified = false
  override fun apply() {
    myTwitterSettings.setAskToTweet(myAskToTweetCheckBox!!.isSelected)
  }

  override fun reset() {
    myTwitterSettings.setAskToTweet(true)
  }

  override fun disposeUIResources() {}
  override fun createComponent(): JComponent? {
    val hasCourse = StudyTaskManager.getInstance(myProject).course != null
    val twitterConfigurator = EduUtils.getTwitterConfigurator(myProject)
    return if (hasCourse && twitterConfigurator != null) {
      myPanel
    }
    else null
  }

  override fun isModified(): Boolean {
    return myIsModified
  }

  @Nls
  override fun getDisplayName(): String {
    return "Twitter Settings"
  }

  init {
    myTwitterSettings = KtTwitterSettings.getInstance(myProject)
    val twitterConfigurator = EduUtils.getTwitterConfigurator(myProject)
    if (twitterConfigurator != null) {
      myAskToTweetCheckBox!!.isSelected = myTwitterSettings.askToTweet()
    }
    myAskToTweetCheckBox!!.addActionListener { e: ActionEvent? -> myIsModified = true }
  }
}