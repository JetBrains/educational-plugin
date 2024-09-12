package com.jetbrains.edu.cpp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.edu.learning.courseDir

// BACKCOMPAT: 2024.1. Inline it.
fun refreshCMakeProject(project: Project) {
  ApplicationManager.getApplication().executeOnPooledThread {
    //TODO(launch coroutine properly)
    runBlockingMaybeCancellable {
      // if it is a new project it will be initialized, else it will be reloaded only.
      CMakeWorkspace.getInstance(project).linkCMakeProject(VfsUtil.virtualToIoFile(project.courseDir))
    }
  }
}