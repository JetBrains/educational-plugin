package com.jetbrains.edu.android

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.checker.gradle.GradleTaskCheckerProvider

class AndroidTaskCheckerProvider : GradleTaskCheckerProvider() {
  override fun mainClassForFile(project: Project, file: VirtualFile): String? = null
}
