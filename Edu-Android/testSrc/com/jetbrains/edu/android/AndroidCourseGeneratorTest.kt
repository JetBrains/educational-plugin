package com.jetbrains.edu.android

import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.*
import org.hamcrest.CoreMatchers
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Assert.assertThat

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

    val gradleProperties = findFile("gradle.properties")
    val text = VfsUtil.loadText(gradleProperties)
    listOf(
      "android.enableJetifier=true",
      "android.useAndroidX=true",
      "org.gradle.jvmargs=-Xmx1536m"
    ).forEach { line ->
      assertThat(text, CoreMatchers.containsString(line))
    }
  }

  fun `test do not rewrite already created additional files`() {
    val course = course(language = KotlinLanguage.INSTANCE, environment = EduNames.ANDROID) {
      additionalFile("gradle.properties", "some.awesome.property=true")
    }
    createCourseStructure(course)

    fileTree {
      file("local.properties")
      file("gradle.properties", "some.awesome.property=true")
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }
}
