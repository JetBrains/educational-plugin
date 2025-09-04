package com.jetbrains.edu.coursecreator.archive

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.CourseFileAttributes
import com.jetbrains.edu.learning.configuration.CourseViewVisibility
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

data class ExpectedCourseFileAttributes(
  val excludedFromArchive: Boolean? = null,
  val archiveInclusionPolicy: ArchiveInclusionPolicy? = null,
  val visibility: CourseViewVisibility? = null
) {
  fun assertAttributes(actual: CourseFileAttributes) {
    if (excludedFromArchive != null) {
      assertEquals("Excluded from archive attribute mismatch", excludedFromArchive, actual.excludedFromArchive)
    }
    if (archiveInclusionPolicy != null) {
      assertEquals("Archive inclusion policy attribute mismatch", archiveInclusionPolicy, actual.archiveInclusionPolicy)
    }
    if (visibility != null) {
      assertEquals("Course view visibility attribute mismatch", visibility, actual.visibility)
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
      excludedFromArchive: Boolean? = null,
      archiveInclusionPolicy: ArchiveInclusionPolicy? = null,
      visibility: CourseViewVisibility? = null
    ): ExpectedCourseFileAttributes = ExpectedCourseFileAttributes(
      excludedFromArchive=excludedFromArchive,
      archiveInclusionPolicy=archiveInclusionPolicy,
      visibility=visibility
    )

    fun doTest(configurator: EduConfigurator<*>, filePath: String, expectedAttributes: ExpectedCourseFileAttributes) {
      val attributesEvaluator = configurator.courseFileAttributesEvaluator

      val isDirectory = filePath.last() == '/'
      val relativePath = filePath.removeSuffix("/")
      val actualAttributes = attributesEvaluator.attributesForPath(relativePath, isDirectory)

      expectedAttributes.assertAttributes(actualAttributes)
    }

    /**
     * Concatenates two collections of the form `[array(file1, ...), array(file2, ...)]` in such a way, that
     * if the second collection contains `fileN` from the first collection, only the corresponding array from the second collection is taken.
     */
    fun Collection<Array<Any>>.extend(vararg elements: Array<Any>): Collection<Array<Any>> =
      this.filter { thisElement -> elements.find { thisElement[0] == it[0] } == null } + elements

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      val excluded = expected(
        excludedFromArchive = true,
        archiveInclusionPolicy = ArchiveInclusionPolicy.MUST_EXCLUDE,
        visibility = CourseViewVisibility.AUTHOR_DECISION
      )
      val excludedButCanBeInside = expected(
        excludedFromArchive = true,
        archiveInclusionPolicy = ArchiveInclusionPolicy.AUTHOR_DECISION,
        visibility = CourseViewVisibility.AUTHOR_DECISION
      )
      val normal = expected(
        excludedFromArchive = false,
        archiveInclusionPolicy = ArchiveInclusionPolicy.AUTHOR_DECISION,
        visibility = CourseViewVisibility.AUTHOR_DECISION
      )
      val excludedAndInvisible = excluded.copy(visibility = CourseViewVisibility.INVISIBLE_FOR_ALL)
      val normalAndInvisible = normal.copy(visibility = CourseViewVisibility.INVISIBLE_FOR_ALL)

      return listOf(
        arrayOf("regular-file", normal),
        arrayOf("regular-folder/", normal),
        arrayOf("regular-file/inside-a-folder", normal),
        arrayOf("regular-folder/inside-a-folder/", normal),

        //.idea contents
        arrayOf(".idea/", normal),
        arrayOf(".idea/subfile", excluded),
        arrayOf(".idea/subfolder/", excluded),
        arrayOf(".idea/inspectionProfiles/", normal),
        arrayOf(".idea/scopes/", normal),
        arrayOf(".idea/scopes/subfile", normal),

        //.dot files and folders
        arrayOf(".folder/", excludedButCanBeInside),
        arrayOf(".file", excludedButCanBeInside),
        arrayOf(".folder/in/subfolder/", excludedButCanBeInside),
        arrayOf(".file/in/subfolder", excludedButCanBeInside),
        arrayOf("folder/.subfile", excludedButCanBeInside),
        arrayOf("folder/.subfolder/", excludedButCanBeInside),
        arrayOf("folder/.subfolder/subfile", excludedButCanBeInside),
        arrayOf(".idea/scopes/.excluded_with_dot", excludedButCanBeInside),

        // iml files
        arrayOf("project.iml", excludedAndInvisible),
        arrayOf("subfolder/project.iml", excludedAndInvisible),

        // task descriptions
        arrayOf("lesson/task/task.md", excluded),
        arrayOf("section/lesson/task/task.md", excluded),
        arrayOf("lesson/task/task.html", excluded),
        arrayOf("section/lesson/task/task.html", excluded),

        // configs
        arrayOf("course-info.yaml", excluded),
        arrayOf("section/section-info.yaml", excluded),
        arrayOf("lesson/lesson-info.yaml", excluded),
        arrayOf("section/lesson/lesson-info.yaml", excluded),
        arrayOf("lesson/task/task-info.yaml", excluded),
        arrayOf("section/lesson/task/task-info.yaml", excluded),

        // remote configs
        arrayOf("course-remote-info.yaml", excluded),
        arrayOf("section/section-remote-info.yaml", excluded),
        arrayOf("lesson/lesson-remote-info.yaml", excluded),
        arrayOf("section/lesson/lesson-remote-info.yaml", excluded),
        arrayOf("lesson/task/task-remote-info.yaml", excluded),
        arrayOf("section/lesson/task/task-remote-info.yaml", excluded),

        //.coursecreator
        arrayOf(".coursecreator/archive.zip", excludedAndInvisible),

        //vcs
        arrayOf(".git/objects/ha/hahaha42e136b17b7adfe79921a7a17def1185", excludedAndInvisible),
        arrayOf(".git/config", excludedAndInvisible),

        // other
        arrayOf("hints", excludedButCanBeInside),
        arrayOf("stepik_ids.json", excludedButCanBeInside),
        arrayOf(".courseignore", excluded),
        arrayOf("courseIcon.svg", excluded),

        arrayOf("build/", normalAndInvisible),
        arrayOf("out/", normalAndInvisible),
        arrayOf("dir/build/", normalAndInvisible),
        arrayOf("dir/out/", normalAndInvisible),
      )
    }
  }
}