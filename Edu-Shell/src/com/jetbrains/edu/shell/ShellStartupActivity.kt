package com.jetbrains.edu.shell

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.sh.ShLanguage
import com.intellij.sh.shellcheck.ShShellcheckUtil
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.languageById

@Suppress("UsagesOfObsoleteApi") // BACKCOMPAT 223: use com.intellij.openapi.startup.ProjectActivity
class ShellStartupActivity : StartupActivity.DumbAware {
  override fun runActivity(project: Project) {
    if (project.course?.languageById != ShLanguage.INSTANCE) return

    val onSuccess = Runnable {
      runReadAction {
        if (project.isDisposed) return@runReadAction
        EditorNotifications.getInstance(project).updateAllNotifications()
      }
    }
    val onFailure = Runnable {  }
    ShShellcheckUtil.download(project, onSuccess, onFailure)
  }
}