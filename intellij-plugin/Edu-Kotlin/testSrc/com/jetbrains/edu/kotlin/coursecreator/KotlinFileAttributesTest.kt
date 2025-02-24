package com.jetbrains.edu.kotlin.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.jvm.coursecreator.GradleFileAttributesTest
import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.learning.configuration.EduConfigurator
import org.junit.runners.Parameterized.Parameters

class KotlinFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : GradleFileAttributesTest(filePath, expectedAttributes) {

  override val configurator: EduConfigurator<*> = KtConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = GradleFileAttributesTest.data() // there are no special kotlin rules
  }
}