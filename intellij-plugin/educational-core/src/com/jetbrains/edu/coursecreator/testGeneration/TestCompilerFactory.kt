package com.jetbrains.edu.coursecreator.testGeneration

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import org.jetbrains.research.testspark.core.data.JUnitVersion
import org.jetbrains.research.testspark.core.test.TestCompiler

class TestCompilerFactory {
  companion object {
    fun createJavacTestCompiler(project: Project, junitVersion: JUnitVersion): TestCompiler {
      val javaHomePath = ProjectRootManager.getInstance(project).projectSdk!!.homeDirectory!!.path // TODO replace with the real code`
      val libraryPaths = LibraryPathsProvider.getTestCompilationLibraryPaths()
      val junitLibraryPaths = LibraryPathsProvider.getJUnitLibraryPaths(junitVersion)
      println("$$$$1")
      println(javaHomePath)
      println("$$$$2")
      println(libraryPaths)
      println("$$$$3")
      println(junitLibraryPaths)
      return TestCompiler(javaHomePath, libraryPaths, junitLibraryPaths)
    }
  }
}
