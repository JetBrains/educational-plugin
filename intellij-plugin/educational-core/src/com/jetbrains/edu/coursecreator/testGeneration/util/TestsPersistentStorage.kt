package com.jetbrains.edu.coursecreator.testGeneration.util

import org.jetbrains.research.testspark.core.test.TestsPersistentStorage
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

object TestsPersistentStorage : TestsPersistentStorage {
  override fun saveGeneratedTest(packageString: String, code: String, resultPath: String, testFileName: String): String {

    val generatedTestPath = buildString {
      append("$resultPath${File.separatorChar}")
      packageString.split(".").forEach { directory ->
        if (directory.isNotBlank()) append("$directory${File.separatorChar}")
      }
    }
    Path(generatedTestPath).createDirectories()
    val testFile = File("$generatedTestPath$testFileName")
    testFile.createNewFile()
    testFile.writeText(code)

    return "$generatedTestPath$testFileName"
  }
}
