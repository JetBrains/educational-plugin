package com.jetbrains.edu.csharp

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest.Companion.doTest
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest.Companion.expected
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.CourseViewVisibility
import com.jetbrains.rider.test.BaseIntegrationTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class CSharpFileAttributesTest(
  private val filePath: String,
  private val expectedAttributes: ExpectedCourseFileAttributes
) : BaseIntegrationTest() {

  @Test
  fun `file has correct course attributes`() {
    doTest(configurator, filePath, expectedAttributes)
  }

  companion object {
    val configurator = CSharpConfigurator()

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      val notInArchive = expected(
        excludedFromArchive = false,
        archiveInclusionPolicy = ArchiveInclusionPolicy.MUST_EXCLUDE,
        visibility = CourseViewVisibility.AUTHOR_DECISION
      )
      val notInArchiveAndExcluded = notInArchive.copy(excludedFromArchive = true)

      return FileAttributesTest.data() + listOf(
        arrayOf("a.sln", notInArchiveAndExcluded),
        arrayOf("folder/a.sln", notInArchiveAndExcluded),
        arrayOf("lesson1/task1/a.sln", notInArchiveAndExcluded),

        arrayOf("obj/", notInArchiveAndExcluded),
        arrayOf("bin/", notInArchiveAndExcluded),
        arrayOf("obj/some/file/inside", notInArchiveAndExcluded),
        arrayOf("bin/some/file/inside", notInArchiveAndExcluded),
        arrayOf("lesson1/task1/obj/", notInArchiveAndExcluded),
        arrayOf("lesson1/task1/bin/", notInArchiveAndExcluded),
      )
    }
  }
}