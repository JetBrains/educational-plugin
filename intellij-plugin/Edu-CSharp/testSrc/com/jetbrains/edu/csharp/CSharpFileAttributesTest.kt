package com.jetbrains.edu.csharp

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest.Companion.doTest
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest.Companion.expected
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
class CSharpFileAttributesTest(
  private val filePath: String,
  private val expectedAttributes: ExpectedCourseFileAttributes
) : CSharpTestBase() {

  @Test
  fun `file has correct course attributes`() {
    doTest(configurator, filePath, expectedAttributes)
  }

  companion object {
    val configurator = CSharpConfigurator()

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      val expectedAttributes = expected(
        excludedFromArchive = true,
        archiveInclusionPolicy = ArchiveInclusionPolicy.MUST_EXCLUDE
      )

      return FileAttributesTest.data() + listOf(
        arrayOf("a.sln", expectedAttributes),
        arrayOf("folder/a.sln", expectedAttributes),
        arrayOf("lesson1/task1/a.sln", expectedAttributes),

        arrayOf("obj/", expectedAttributes),
        arrayOf("bin/", expectedAttributes),
        arrayOf("obj/some/file/inside", expectedAttributes),
        arrayOf("bin/some/file/inside", expectedAttributes),
        arrayOf("lesson1/task1/obj/", expectedAttributes),
        arrayOf("lesson1/task1/bin/", expectedAttributes),
      )
    }
  }
}