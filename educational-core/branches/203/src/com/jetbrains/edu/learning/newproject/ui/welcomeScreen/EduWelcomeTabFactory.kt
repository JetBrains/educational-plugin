package com.jetbrains.edu.learning.newproject.ui.welcomeScreen

import com.intellij.openapi.Disposable
import com.intellij.openapi.wm.WelcomeScreenTab
import com.intellij.openapi.wm.WelcomeTabFactory
import com.intellij.openapi.wm.impl.welcomeScreen.TabbedWelcomeScreen.DefaultWelcomeScreenTab
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JComponent

class EduWelcomeTabFactory : WelcomeTabFactory {
  override fun createWelcomeTab(parentDisposable: Disposable): WelcomeScreenTab {
    return object : DefaultWelcomeScreenTab(EduCoreBundle.message("course.dialog.my.courses")) {
      override fun buildComponent(): JComponent {
        return EduWelcomeTabPanel()
      }
    }
  }
}