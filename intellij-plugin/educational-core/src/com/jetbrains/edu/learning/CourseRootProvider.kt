package com.jetbrains.edu.learning

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile

interface CourseRootProvider {
  fun Project.getCourseDir(): VirtualFile?

  companion object {
    val EP_NAME = ExtensionPointName.create<CourseRootProvider>("Educational.courseRootProvider")
    fun Project.getCourseRootDir(): VirtualFile? {
      val point = EP_NAME.extensionsIfPointIsRegistered.firstOrNull() ?: return guessProjectDir()
      return with(point) {
        this@getCourseRootDir.getCourseDir()
      }
    }
  }
}