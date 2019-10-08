package com.jetbrains.edu.android

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.CourseGenerationTestBase
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse
import org.jetbrains.kotlin.idea.KotlinLanguage

class AndroidCourseGeneratorTest : CourseGenerationTestBase<JdkProjectSettings>() {

  override val defaultSettings: JdkProjectSettings get() = JdkProjectSettings.emptySettings()

  fun `test new course structure`() {
    val course = newCourse(KotlinLanguage.INSTANCE, environment = EduNames.ANDROID)
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
