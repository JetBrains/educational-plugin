package com.jetbrains.edu.coursecreator.testGeneration

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.task.ProjectTaskManager
import org.jetbrains.research.testspark.core.utils.DataFilesUtil
import java.io.File

object TestBuildUtil {

  fun getBuildPath(project: Project): String {

    var buildPath = ""

    for (module in ModuleManager.getInstance(project).modules) {

      val compilerOutputPath = CompilerModuleExtension.getInstance(module)?.compilerOutputPath

      compilerOutputPath?.let { buildPath += compilerOutputPath.path.plus(DataFilesUtil.classpathSeparator.toString()) }
      // Include extra libraries in classpath
      val librariesPaths = ModuleRootManager.getInstance(module).orderEntries().librariesOnly().pathsList.pathList
      for (lib in librariesPaths) {
        // exclude the invalid classpaths
        if (buildPath.contains(lib)) {
          continue
        }
        if (lib.endsWith(".zip")) {
          continue
        }

        // remove junit and hamcrest libraries, since we use our own libraries
        val pathArray = lib.split(File.separatorChar)
        val libFileName = pathArray[pathArray.size - 1]
        if (libFileName.startsWith("junit") ||
            libFileName.startsWith("hamcrest") //TODO to settings
        ) {
          continue
        }

        buildPath += lib.plus(DataFilesUtil.classpathSeparator.toString())
      }
    }
    return buildPath
  }

  fun getResultPath(id: String, testResultDirectory: String): String {
    val testResultName = "test_gen_result_$id" // TODO

    return "$testResultDirectory$testResultName"
  }
}