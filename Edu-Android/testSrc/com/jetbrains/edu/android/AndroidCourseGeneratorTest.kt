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
      dir("lesson1/task1") {
        dir("src") {
          dir("main") {
            dir("java/com/example/android/course") {
              file("MainActivity.kt")
            }
            dir("res") {
              dir("layout") {
                file("activity_main.xml")
              }
              dir("values") {
                file("styles.xml")
                file("strings.xml")
                file("colors.xml")
              }
            }
            file("AndroidManifest.xml")
          }
          dir("test/java/com/example/android/course") {
            file("ExampleUnitTest.kt")
          }
          dir("androidTest/java/com/example/android/course") {
            file("AndroidEduTestRunner.kt")
            file("ExampleInstrumentedTest.kt")
          }
        }
        file("task.md")
        file("build.gradle")
      }
      gradleWrapperFiles()
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
      gradleWrapperFiles()
      file("local.properties")
      file("gradle.properties", "some.awesome.property=true")
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }

  private fun FileTreeBuilder.gradleWrapperFiles() {
    dir("gradle/wrapper") {
      file("gradle-wrapper.jar")
      file("gradle-wrapper.properties")
    }
    file("gradlew")
    file("gradlew.bat")
  }
}
