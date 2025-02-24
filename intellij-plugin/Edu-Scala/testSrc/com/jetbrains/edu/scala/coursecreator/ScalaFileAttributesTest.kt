package com.jetbrains.edu.scala.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.jvm.coursecreator.GradleFileAttributesTest
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.scala.gradle.ScalaGradleConfigurator
import org.junit.runners.Parameterized.Parameters

class ScalaFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : GradleFileAttributesTest(filePath, expectedAttributes) {

  override val configurator: EduConfigurator<*> = ScalaGradleConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = GradleFileAttributesTest.data() // there are no special scala rules
  }
}