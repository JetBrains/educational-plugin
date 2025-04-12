package com.jetbrains.edu.rust.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
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
    fun data(): Collection<Array<Any>> {
      val insideArchive = expected(
        excludedFromArchive = false,
        archiveInclusionPolicy = ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT
      )

      return FileAttributesTest.data() + listOf(
        arrayOf(".cargo/", insideArchive),
        arrayOf(".cargo/${CargoConstants.CONFIG_TOML_FILE}", insideArchive),
        arrayOf(".cargo/${CargoConstants.CONFIG_FILE}", insideArchive),

        arrayOf(
          ".cargo/other-file",
          expected(excludedFromArchive = true, archiveInclusionPolicy = ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
        ),
        arrayOf(
          "Cargo.toml",
          expected(excludedFromArchive = false, archiveInclusionPolicy = ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
        ),
      )
    }
  }
}