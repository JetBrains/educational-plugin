package com.jetbrains.edu.jvm.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest

abstract class GradleFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {

  companion object {
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