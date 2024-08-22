package com.jetbrains.edu.coursecreator.testGeneration

import com.intellij.openapi.application.PathManager
import org.jetbrains.research.testspark.core.data.JUnitVersion
import org.jetbrains.research.testspark.core.test.data.dependencies.JavaTestCompilationDependencies
import java.io.File

class LibraryPathsProvider {
  companion object {
    private val sep = File.separatorChar
    val libPrefix = "${PathManager.getPluginsPath()}${sep}JetBrainsAcademy${sep}lib" // TODO

    fun getTestCompilationLibraryPaths() = JavaTestCompilationDependencies.getJarDescriptors().map { descriptor ->
      "$libPrefix${sep}${descriptor.name}"
    }

    fun getJUnitLibraryPaths(junitVersion: JUnitVersion): List<String> = junitVersion.libJar.map { descriptor ->
      "$libPrefix${sep}${descriptor.name}"
    }
  }
}
