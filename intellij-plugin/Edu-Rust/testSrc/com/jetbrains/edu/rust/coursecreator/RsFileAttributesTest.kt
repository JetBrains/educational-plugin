package com.jetbrains.edu.rust.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.rust.RsConfigurator
import org.junit.runners.Parameterized.Parameters
import org.rust.cargo.CargoConstants

class RsFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {
  override val configurator: EduConfigurator<*> = RsConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = FileAttributesTest.data() + listOf(
      arrayOf(".cargo/", expected(excludedFromArchive = false)),
      arrayOf(".cargo/${CargoConstants.CONFIG_TOML_FILE}", expected(excludedFromArchive = false)),
      arrayOf(".cargo/${CargoConstants.CONFIG_FILE}", expected(excludedFromArchive = false)),

      arrayOf(".cargo/other-file", expected(excludedFromArchive = true)),
    )
  }
}