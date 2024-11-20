package com.jetbrains.edu.csharp.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.CourseRootProvider
import com.jetbrains.rider.plugins.unity.isUnityProject

class HyperskillUnityCourseRootProvider : CourseRootProvider {
  override fun Project.findCourseRootIfNeeded(): VirtualFile? {
    if (isUnityProject.value) {
      return guessProjectDir()?.findFileByRelativePath("$PACKAGES_DIR/$HYPERSKILL_UNITY_NAME/$TESTS_DIR")
             ?: error("Invalid structure for Unity-based course")
    }
    return null
  }

  companion object {
    private const val PACKAGES_DIR = "Packages"
    private const val HYPERSKILL_UNITY_NAME: String = "HyperskillUnityTests"
    private const val TESTS_DIR = "Tests"
  }
}