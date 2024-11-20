package com.jetbrains.edu.learning

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface CourseRootProvider {
  fun Project.findCourseRootIfNeeded(): VirtualFile?

  companion object {
    val EP_NAME = ExtensionPointName.create<CourseRootProvider>("Educational.courseRootProvider")
    fun Project.getCourseRootDir(): VirtualFile? = EP_NAME.computeSafeIfAny {
      with(it) {
        this@getCourseRootDir.findCourseRootIfNeeded()
      }
    }
  }
}