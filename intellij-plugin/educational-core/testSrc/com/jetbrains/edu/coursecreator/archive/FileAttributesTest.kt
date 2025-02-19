package com.jetbrains.edu.coursecreator.archive

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configuration.CourseFileAttributes
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

data class ExpectedCourseFileAttributes(
  val excludedFromArchive: Boolean? = null
) {
  fun assertAttributes(actual: CourseFileAttributes) {
    if (excludedFromArchive != null) {
      assertEquals("Excluded from archive attribute mismatch", excludedFromArchive, actual.excludedFromArchive)
    }
  }
}

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
open class FileAttributesTest(
  private val filePath: String,
  private val expectedAttributes: ExpectedCourseFileAttributes
) : EduTestCase() {

  protected open val configurator: EduConfigurator<*> = PlainTextConfigurator()

  @Test
  fun `file has correct course attributes`() = doTest(configurator, filePath, expectedAttributes)

  companion object {
    fun expected(
      excludedFromArchive: Boolean? = null
    ): ExpectedCourseFileAttributes = ExpectedCourseFileAttributes(
      excludedFromArchive=excludedFromArchive
    )

    fun doTest(configurator: EduConfigurator<*>, filePath: String, expectedAttributes: ExpectedCourseFileAttributes) {
      val attributesEvaluator = configurator.courseFileAttributesEvaluator

      val isDirectory = filePath.last() == '/'
      val relativePath = filePath.removeSuffix("/")
      val actualAttributes = attributesEvaluator.attributesForPath(relativePath, isDirectory)

      expectedAttributes.assertAttributes(actualAttributes)
    }

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = listOf(
      arrayOf("regular-file", expected(excludedFromArchive = false)),
      arrayOf("regular-folder/", expected(excludedFromArchive = false)),
      arrayOf("regular-file/inside-a-folder", expected(excludedFromArchive = false)),
      arrayOf("regular-folder/inside-a-folder/", expected(excludedFromArchive = false)),

      //.idea contents
      arrayOf(".idea/", expected(excludedFromArchive = false)),
      arrayOf(".idea/subfile", expected(excludedFromArchive = true)),
      arrayOf(".idea/subfolder/", expected(excludedFromArchive = true)),
      arrayOf(".idea/inspectionProfiles/", expected(excludedFromArchive = false)),
      arrayOf(".idea/scopes/", expected(excludedFromArchive = false)),
      arrayOf(".idea/scopes/subfile", expected(excludedFromArchive = false)),

      //.dot files and folders
      arrayOf(".folder/", expected(excludedFromArchive = true)),
      arrayOf(".file", expected(excludedFromArchive = true)),
      arrayOf(".folder/in/subfolder/", expected(excludedFromArchive = true)),
      arrayOf(".file/in/subfolder", expected(excludedFromArchive = true)),
      arrayOf("folder/.subfile", expected(excludedFromArchive = true)),
      arrayOf("folder/.subfolder/", expected(excludedFromArchive = true)),
      arrayOf("folder/.subfolder/subfile", expected(excludedFromArchive = true)),
      arrayOf(".idea/scopes/.excluded_with_dot", expected(excludedFromArchive = true)),

      // iml files
      arrayOf("project.iml", expected(excludedFromArchive = true)),
      arrayOf("subfolder/project.iml", expected(excludedFromArchive = true)),

      // task descriptions
      arrayOf("lesson/task/task.md", expected(excludedFromArchive = true)),
      arrayOf("section/lesson/task/task.md", expected(excludedFromArchive = true)),
      arrayOf("lesson/task/task.html", expected(excludedFromArchive = true)),
      arrayOf("section/lesson/task/task.html", expected(excludedFromArchive = true)),

      // configs
      arrayOf("course-info.yaml", expected(excludedFromArchive = true)),
      arrayOf("section/section-info.yaml", expected(excludedFromArchive = true)),
      arrayOf("lesson/lesson-info.yaml", expected(excludedFromArchive = true)),
      arrayOf("section/lesson/lesson-info.yaml", expected(excludedFromArchive = true)),
      arrayOf("lesson/task/task-info.yaml", expected(excludedFromArchive = true)),
      arrayOf("section/lesson/task/task-info.yaml", expected(excludedFromArchive = true)),

      // remote configs
      arrayOf("course-remote-info.yaml", expected(excludedFromArchive = true)),
      arrayOf("section/section-remote-info.yaml", expected(excludedFromArchive = true)),
      arrayOf("lesson/lesson-remote-info.yaml", expected(excludedFromArchive = true)),
      arrayOf("section/lesson/lesson-remote-info.yaml", expected(excludedFromArchive = true)),
      arrayOf("lesson/task/task-remote-info.yaml", expected(excludedFromArchive = true)),
      arrayOf("section/lesson/task/task-remote-info.yaml", expected(excludedFromArchive = true)),

      //.coursecreator
      arrayOf(".coursecreator/archive.zip", expected(excludedFromArchive = true)),

      // other
      arrayOf("hints", expected(excludedFromArchive = true)),
      arrayOf("stepik_ids.json", expected(excludedFromArchive = true)),
      arrayOf(".courseignore", expected(excludedFromArchive = true)),
      arrayOf("courseIcon.svg", expected(excludedFromArchive = true)),
    )
  }
}