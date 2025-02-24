package com.jetbrains.edu.java.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.java.JConfigurator
import com.jetbrains.edu.jvm.coursecreator.GradleFileAttributesTest
import com.jetbrains.edu.learning.configuration.EduConfigurator
import org.junit.runners.Parameterized.Parameters

class JavaFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : GradleFileAttributesTest(filePath, expectedAttributes) {

  override val configurator: EduConfigurator<*> = JConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = GradleFileAttributesTest.data() // there are no special java rules
  }
}