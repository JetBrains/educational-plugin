package com.jetbrains.edu.jvm.coursecreator

import com.jetbrains.edu.coursecreator.archive.ExpectedCourseFileAttributes
import com.jetbrains.edu.coursecreator.archive.FileAttributesTest
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.CourseViewVisibility

abstract class GradleFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {

  companion object {
    fun data(): Collection<Array<Any>> {
      val inArchive = expected(
        excludedFromArchive = false,
        archiveInclusionPolicy = ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT,
        visibility = CourseViewVisibility.AUTHOR_DECISION
      )
      val outsideArchive = expected(
        excludedFromArchive = true,
        archiveInclusionPolicy = ArchiveInclusionPolicy.MUST_EXCLUDE,
        visibility = CourseViewVisibility.INVISIBLE_FOR_ALL
      )

      return FileAttributesTest.data().extend(
        arrayOf("settings.gradle", inArchive),
        arrayOf("subfolder/settings.gradle", inArchive),

        arrayOf("out/", outsideArchive),
        arrayOf("subfolder/out/", outsideArchive),
        arrayOf("subfolder/out/subfile", outsideArchive),

        arrayOf("build/", outsideArchive),
        arrayOf("subfolder/build/", outsideArchive),
        arrayOf("subfolder/build/subfile", outsideArchive),

        arrayOf("gradle/", outsideArchive),
        arrayOf("subfolder/gradle/", outsideArchive),
        arrayOf("subfolder/gradle/subfile", outsideArchive),

        arrayOf("EduTestRunner.java", outsideArchive),
        arrayOf("gradlew", outsideArchive),
        arrayOf("gradlew.bat", outsideArchive),
        arrayOf("local.properties", outsideArchive),
        arrayOf("gradle-wrapper.jar", outsideArchive),
        arrayOf("gradle-wrapper.properties", outsideArchive),

        arrayOf("subfolder/EduTestRunner.java", outsideArchive),
        arrayOf("subfolder/gradlew", outsideArchive),
        arrayOf("subfolder/gradlew.bat", outsideArchive),
        arrayOf("subfolder/local.properties", outsideArchive),
        arrayOf("subfolder/gradle-wrapper.jar", outsideArchive),
        arrayOf("subfolder/gradle-wrapper.properties", outsideArchive),

        // override 'build' and 'out' directories from the FileAttributesTest.data()
        arrayOf("build/", outsideArchive),
        arrayOf("out/", outsideArchive),
        arrayOf("dir/build/", outsideArchive),
        arrayOf("dir/out/", outsideArchive),

        arrayOf(".gradle/", outsideArchive),
        arrayOf(".gradle/file", outsideArchive),
      )
    }
  }
}