package com.jetbrains.edu.android

import com.jetbrains.edu.learning.CourseGenerationTestBase
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.newCourse

class AndroidCourseGeneratorTest : CourseGenerationTestBase<JdkProjectSettings>() {

  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = AndroidCourseBuilder()
  override val defaultSettings: JdkProjectSettings get() = JdkProjectSettings.emptySettings()

  fun `test new course structure`() {
    val course = newCourse(Android)
    createCourseStructure(courseBuilder, course, defaultSettings)

    val expectedFileTree = fileTree {
      file("local.properties")
      file("build.gradle")
      file("settings.gradle")
    }

    expectedFileTree.assertEquals(rootDir)
  }
}
