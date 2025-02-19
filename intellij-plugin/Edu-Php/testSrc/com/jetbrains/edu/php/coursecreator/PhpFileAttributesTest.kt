package com.jetbrains.edu.php.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
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
    fun data(): Collection<Array<Any>> = FileAttributesTest.data() + listOf(
      arrayOf("vendor/", expected(excludedFromArchive = true)),
      arrayOf("subfolder/vendor/", expected(excludedFromArchive = true)),
      arrayOf("subfolder/vendor/subfile", expected(excludedFromArchive = true)),

      arrayOf("composer.phar", expected(excludedFromArchive = true)),
      arrayOf("subfolder/composer.phar", expected(excludedFromArchive = true)),
    )
  }
}