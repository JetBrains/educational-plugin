package com.jetbrains.edu.learning

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile

val isUnitTestMode: Boolean get() = ApplicationManager.getApplication().isUnitTestMode

fun checkIsBackgroundThread() {
  check(!ApplicationManager.getApplication().isDispatchThread) {
    "Long running operation invoked on UI thread"
  }
}

val Project.courseDir: VirtualFile get() {
  return guessProjectDir() ?: error("Failed to find course dir for $this")
}
