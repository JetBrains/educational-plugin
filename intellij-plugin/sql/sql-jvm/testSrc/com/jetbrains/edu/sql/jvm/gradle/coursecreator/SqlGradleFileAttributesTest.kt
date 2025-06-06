package com.jetbrains.edu.sql.jvm.gradle.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
import com.jetbrains.edu.jvm.coursecreator.GradleFileAttributesTest
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.sql.jvm.gradle.SqlGradleConfigurator
import org.junit.runners.Parameterized.Parameters
import kotlin.collections.plus

class SqlGradleFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {
  override val configurator: EduConfigurator<*> = SqlGradleConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      val expectedAttributes = expected(
        excludedFromArchive = true,
        archiveInclusionPolicy = ArchiveInclusionPolicy.MUST_EXCLUDE
        )

      return GradleFileAttributesTest.data() + listOf(
        arrayOf("file.db", expectedAttributes),
        arrayOf("subfolder/file.db", expectedAttributes),
      )
    }
  }
}