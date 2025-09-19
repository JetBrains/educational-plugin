package com.jetbrains.edu.scala.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.CourseViewVisibility
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.scala.sbt.ScalaSbtConfigurator
import org.junit.runners.Parameterized.Parameters
import kotlin.collections.plus

class ScalaSbtFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {
  override val configurator: EduConfigurator<*> = ScalaSbtConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      val excluded = expected(
        excludedFromArchive = true,
        visibility = CourseViewVisibility.INVISIBLE_FOR_ALL,
        archiveInclusionPolicy = ArchiveInclusionPolicy.MUST_EXCLUDE
      )
      val included = expected(
        excludedFromArchive = false,
        visibility = CourseViewVisibility.AUTHOR_DECISION,
        archiveInclusionPolicy = ArchiveInclusionPolicy.SHOULD_BE_INCLUDED
      )

      return FileAttributesTest.data() + listOf(
        arrayOf("target/", excluded),
        arrayOf("subfolder/target/", excluded),
        arrayOf("subfolder/target/subfile", excluded),

        arrayOf("project/build.properties", included),
        arrayOf("build.sbt", included),
        arrayOf("dir/build.sbt", included),
      )
    }
  }
}