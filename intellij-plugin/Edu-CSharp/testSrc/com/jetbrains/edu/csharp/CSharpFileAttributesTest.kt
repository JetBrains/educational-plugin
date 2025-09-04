package com.jetbrains.edu.csharp

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest.Companion.doTest
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest.Companion.expected
import com.jetbrains.edu.csharp.hyperskill.CSharpHyperskillConfigurator
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.CourseViewVisibility
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
    val configurator = CSharpHyperskillConfigurator()

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      val notInArchive = expected(
        excludedFromArchive = false,
        archiveInclusionPolicy = ArchiveInclusionPolicy.MUST_EXCLUDE,
        visibility = CourseViewVisibility.AUTHOR_DECISION
      )
      val notInArchiveAndExcluded = notInArchive.copy(excludedFromArchive = true)
      val visible = expected(
        excludedFromArchive = false,
        archiveInclusionPolicy = ArchiveInclusionPolicy.AUTHOR_DECISION,
        visibility = CourseViewVisibility.VISIBLE_FOR_STUDENT
      )

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

        arrayOf("a.meta", notInArchive),
        arrayOf("dir/a.meta", notInArchive),

        arrayOf("Packages/", visible),
        arrayOf("dir/Packages/", visible),
        arrayOf("ProjectSettings/", visible),
        arrayOf("dir/ProjectSettings/", visible),
        arrayOf("Assets/", visible),
        arrayOf("dir/Assets/", visible),
        arrayOf("dir/Assets/dir/", visible),
        arrayOf("dir/Assets/file", visible),
      )
    }
  }
}