package com.jetbrains.edu.csharp.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.CourseRootProvider
import com.jetbrains.rider.plugins.unity.isUnityProject

class UnityCourseRootProvider : CourseRootProvider {
  override fun Project.getCourseDir(): VirtualFile? {
    if (isUnityProject.value) {
      return guessProjectDir()?.findChild("Packages")?.findChild(HYPERSKILL_UNITY_NAME)?.findChild("Tests")
    }
    return guessProjectDir()
  }

  companion object {
    const val HYPERSKILL_UNITY_NAME: String = "HyperskillUnityTests"
  }
}