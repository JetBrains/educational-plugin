package com.jetbrains.edu.python.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest.Companion.expected
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.python.learning.PyConfigurator
import com.jetbrains.edu.python.learning.PyNewConfigurator
import org.junit.runners.Parameterized.Parameters
import kotlin.collections.plus

private fun pyData(): Collection<Array<Any>> {
  val expectedAttributes = expected(
    excludedFromArchive = true,
    archiveInclusionPolicy = ArchiveInclusionPolicy.MUST_EXCLUDE
  )

  return FileAttributesTest.data() + listOf(
    arrayOf("file.pyc", expectedAttributes),
    arrayOf("subfolder/file.pyc", expectedAttributes),

    arrayOf("__pycache__/", expectedAttributes),
    arrayOf("__pycache__/subfile", expectedAttributes),
    arrayOf("venv/", expectedAttributes),
    arrayOf("venv/subfile", expectedAttributes),
  )
}

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