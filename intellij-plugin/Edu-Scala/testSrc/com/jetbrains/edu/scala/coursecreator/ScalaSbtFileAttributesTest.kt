package com.jetbrains.edu.scala.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
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
    fun data(): Collection<Array<Any>> = FileAttributesTest.data() + listOf(
      arrayOf("target/", expected(excludedFromArchive = true)),
      arrayOf("subfolder/target/", expected(excludedFromArchive = true)),
      arrayOf("subfolder/target/subfile", expected(excludedFromArchive = true)),
    )
  }
}