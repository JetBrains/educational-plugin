package com.jetbrains.edu.jvm.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import org.junit.runners.Parameterized.Parameters

class GradleFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {
  override val configurator: EduConfigurator<*> = object : GradleConfiguratorBase() {
    override val courseBuilder: GradleCourseBuilderBase
      get() = error("Should not be called")
    override val testFileName: String
      get() = error("Should not be called")
    override val taskCheckerProvider: TaskCheckerProvider
      get() = error("Should not be called")
  }

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = FileAttributesTest.data() + listOf(
      arrayOf("settings.gradle", expected(excludedFromArchive = false)),
      arrayOf("subfolder/settings.gradle", expected(excludedFromArchive = false)),

      arrayOf("out/", expected(excludedFromArchive = true)),
      arrayOf("subfolder/out/", expected(excludedFromArchive = true)),
      arrayOf("subfolder/out/subfile", expected(excludedFromArchive = true)),

      arrayOf("build/", expected(excludedFromArchive = true)),
      arrayOf("subfolder/build/", expected(excludedFromArchive = true)),
      arrayOf("subfolder/build/subfile", expected(excludedFromArchive = true)),

      arrayOf("gradle/", expected(excludedFromArchive = true)),
      arrayOf("subfolder/gradle/", expected(excludedFromArchive = true)),
      arrayOf("subfolder/gradle/subfile", expected(excludedFromArchive = true)),

      arrayOf("EduTestRunner.java", expected(excludedFromArchive = true)),
      arrayOf("gradlew", expected(excludedFromArchive = true)),
      arrayOf("gradlew.bat", expected(excludedFromArchive = true)),
      arrayOf("local.properties", expected(excludedFromArchive = true)),
      arrayOf("gradle-wrapper.jar", expected(excludedFromArchive = true)),
      arrayOf("gradle-wrapper.properties", expected(excludedFromArchive = true)),

      arrayOf("subfolder/EduTestRunner.java", expected(excludedFromArchive = true)),
      arrayOf("subfolder/gradlew", expected(excludedFromArchive = true)),
      arrayOf("subfolder/gradlew.bat", expected(excludedFromArchive = true)),
      arrayOf("subfolder/local.properties", expected(excludedFromArchive = true)),
      arrayOf("subfolder/gradle-wrapper.jar", expected(excludedFromArchive = true)),
      arrayOf("subfolder/gradle-wrapper.properties", expected(excludedFromArchive = true)),
    )
  }
}