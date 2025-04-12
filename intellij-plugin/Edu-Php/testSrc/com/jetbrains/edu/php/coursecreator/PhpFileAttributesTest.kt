package com.jetbrains.edu.php.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.php.PhpConfigurator
import org.junit.runners.Parameterized.Parameters

class PhpFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {
  override val configurator: EduConfigurator<*> = PhpConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      val expectedAttributes = expected(
        excludedFromArchive = true,
        archiveInclusionPolicy = ArchiveInclusionPolicy.MUST_EXCLUDE
      )

      return FileAttributesTest.data() + listOf(
        arrayOf("vendor/", expectedAttributes),
        arrayOf("subfolder/vendor/", expectedAttributes),
        arrayOf("subfolder/vendor/subfile", expectedAttributes),

        arrayOf("composer.phar", expectedAttributes),
        arrayOf("subfolder/composer.phar", expectedAttributes),
      )
    }
  }
}