package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduBrowser.Companion.getInstance
import com.jetbrains.edu.learning.messages.EduCoreBundle.lazyMessage

class LearnMoreAction : DumbAwareAction(lazyMessage("action.learn.more.text")) {
  override fun actionPerformed(e: AnActionEvent) {
    getInstance().browse("https://plugins.jetbrains.com/plugin/10081-jetbrains-academy/docs/jetbrains-academy-plugin.html")
  }
}
