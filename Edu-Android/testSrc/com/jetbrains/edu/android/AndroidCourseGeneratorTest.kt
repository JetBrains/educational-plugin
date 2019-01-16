package com.jetbrains.edu.android

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.gradle.JdkProjectSettings

class AndroidCourseGeneratorTest : CourseGenerationTestBase<JdkProjectSettings>() {

  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = AndroidCourseBuilder()
  override val defaultSettings: JdkProjectSettings get() = JdkProjectSettings.emptySettings()

  fun `test new course structure`() {
    val course = newCourse(PlainTextLanguage.INSTANCE, courseType = EduNames.ANDROID)
    createCourseStructure(course)

    val expectedFileTree = fileTree {
      file("local.properties")
      file("gradle.properties")
      file("build.gradle")
      file("settings.gradle")
    }

    expectedFileTree.assertEquals(rootDir)
  }
}
