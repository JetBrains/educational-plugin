package com.jetbrains.edu.scala.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider

class ScalaGradleTaskCheckerProvider : GradleTaskCheckerProvider() {

  override fun mainClassForFile(project: Project, file: VirtualFile): String? {
    return null
  }
}
