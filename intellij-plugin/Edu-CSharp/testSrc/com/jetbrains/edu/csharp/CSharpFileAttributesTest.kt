package com.jetbrains.edu.csharp

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest.Companion.doTest
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest.Companion.expected
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
    fun data(): Collection<Array<Any>> = FileAttributesTest.data() + listOf(
      arrayOf("a.sln", expected(excludedFromArchive = true)),
      arrayOf("folder/a.sln", expected(excludedFromArchive = true)),
      arrayOf("lesson1/task1/a.sln", expected(excludedFromArchive = true)),

      arrayOf("obj/", expected(excludedFromArchive = true)),
      arrayOf("bin/", expected(excludedFromArchive = true)),
      arrayOf("obj/some/file/inside", expected(excludedFromArchive = true)),
      arrayOf("bin/some/file/inside", expected(excludedFromArchive = true)),
      arrayOf("lesson1/task1/obj/", expected(excludedFromArchive = true)),
      arrayOf("lesson1/task1/bin/", expected(excludedFromArchive = true)),
    )
  }
}