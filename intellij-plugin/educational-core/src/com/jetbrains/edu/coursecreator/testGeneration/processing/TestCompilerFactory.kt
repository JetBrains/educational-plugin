package com.jetbrains.edu.coursecreator.testGeneration.processing

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.edu.coursecreator.testGeneration.util.LibraryPathsProvider
import org.jetbrains.research.testspark.core.data.JUnitVersion
import org.jetbrains.research.testspark.core.test.TestCompiler

class TestCompilerFactory {
  companion object {
    fun createJavacTestCompiler(project: Project, junitVersion: JUnitVersion): TestCompiler {
      val javaHomePath = ProjectRootManager.getInstance(project).projectSdk?.homeDirectory?.path ?: error("project SDK is not selected")
      val libraryPaths = LibraryPathsProvider.getTestCompilationLibraryPaths()
      val junitLibraryPaths = LibraryPathsProvider.getJUnitLibraryPaths(junitVersion)
      return TestCompiler(javaHomePath, libraryPaths, junitLibraryPaths)
    }
  }
}
