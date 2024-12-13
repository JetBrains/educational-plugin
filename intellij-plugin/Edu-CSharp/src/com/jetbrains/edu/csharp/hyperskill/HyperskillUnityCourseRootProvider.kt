package com.jetbrains.edu.csharp.hyperskill

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.CourseRootProvider
import com.jetbrains.rider.plugins.unity.isUnityProject

class HyperskillUnityCourseRootProvider : CourseRootProvider {
  override fun Project.findCourseRootIfNeeded(): VirtualFile? {
    if (!isUnityProject.value) {
      return null
    }

    val projectDir = guessProjectDir()
    val root = projectDir?.findFileByRelativePath("$PACKAGES_DIR/$HYPERSKILL_UNITY_DIR/$UNITY_TESTS_DIR")
               ?: error("Invalid structure for Unity-based course: $projectDir")
    LOG.debug("Unity project root found: $root")
    return root
  }

  companion object {
    private val LOG = Logger.getInstance(HyperskillUnityCourseRootProvider::class.java)

    private const val PACKAGES_DIR = "Packages"
    private const val HYPERSKILL_UNITY_DIR: String = "HyperskillUnityTests"
    private const val UNITY_TESTS_DIR = "Tests"
  }
}