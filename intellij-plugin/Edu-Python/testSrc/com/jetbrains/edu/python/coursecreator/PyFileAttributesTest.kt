package com.jetbrains.edu.python.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest.Companion.expected
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.python.learning.PyConfigurator
import com.jetbrains.edu.python.learning.PyNewConfigurator
import org.junit.runners.Parameterized.Parameters
import kotlin.collections.plus

private fun pyData(): Collection<Array<Any>> = FileAttributesTest.data() + listOf(
  arrayOf("file.pyc", expected(excludedFromArchive = true)),
  arrayOf("subfolder/file.pyc", expected(excludedFromArchive = true)),

  arrayOf("__pycache__/", expected(excludedFromArchive = true)),
  arrayOf("__pycache__/subfile", expected(excludedFromArchive = true)),
  arrayOf("venv/", expected(excludedFromArchive = true)),
  arrayOf("venv/subfile", expected(excludedFromArchive = true)),
)

class PyFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {
  override val configurator: EduConfigurator<*> = PyConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = pyData()
  }
}

class PyNewFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {
  override val configurator: EduConfigurator<*> = PyNewConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = pyData()
  }
}