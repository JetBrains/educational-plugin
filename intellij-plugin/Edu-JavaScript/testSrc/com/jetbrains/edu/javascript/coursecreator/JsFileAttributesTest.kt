package com.jetbrains.edu.javascript.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
import com.jetbrains.edu.javascript.learning.JsConfigurator
import com.jetbrains.edu.learning.configuration.EduConfigurator
import org.junit.runners.Parameterized.Parameters

class JsFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {

  override val configurator: EduConfigurator<*> = JsConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = FileAttributesTest.data() + listOf(
      arrayOf("node_modules/", expected(excludedFromArchive = true)),
      arrayOf("subfolder/node_modules/", expected(excludedFromArchive = true)),
      arrayOf("subfolder/node_modules/subfile", expected(excludedFromArchive = true)),

      arrayOf("package-lock.json", expected(excludedFromArchive = true)),
      arrayOf("subfolder/package-lock.json", expected(excludedFromArchive = true)),
    )
  }
}