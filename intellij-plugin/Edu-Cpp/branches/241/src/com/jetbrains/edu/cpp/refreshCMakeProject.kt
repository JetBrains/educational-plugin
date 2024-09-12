package com.jetbrains.edu.cpp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.edu.learning.courseDir

// BACKCOMPAT: 2024.1. Inline it.
fun refreshCMakeProject(project: Project) {
  // if it is a new project it will be initialized, else it will be reloaded only.
  CMakeWorkspace.getInstance(project).selectProjectDir(VfsUtil.virtualToIoFile(project.courseDir))
}