package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduTestCase
import java.io.File
import java.io.IOException

abstract class CodeforcesTestCase : EduTestCase() {
  override fun getTestDataPath(): String = "testData/codeforces"

  @Throws(IOException::class)
  protected fun loadText(fileName: String): String {
    return FileUtil.loadFile(File(testDataPath, fileName), true)
  }

  companion object {
    const val contest1211 = "Contest 1211.html"
  }
}