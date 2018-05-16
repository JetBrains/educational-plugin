package com.jetbrains.edu.scala

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.checker.gradle.GradleTaskCheckerProvider

class ScalaTaskCheckerProvider : GradleTaskCheckerProvider() {

  override fun mainClassForFile(project: Project, file: VirtualFile): String? {
    return null
  }
}
