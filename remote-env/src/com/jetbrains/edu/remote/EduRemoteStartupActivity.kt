package com.jetbrains.edu.remote

import com.intellij.openapi.project.NOTIFICATIONS_SILENT_MODE
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class EduRemoteStartupActivity : StartupActivity.DumbAware {
  override fun runActivity(project: Project) {
    if (!isRemoteDevServer()) return
    NOTIFICATIONS_SILENT_MODE.set(project, true)
  }
}