package com.jetbrains.edu.android.courseGeneration

import com.android.tools.idea.gradle.project.AndroidGradleProjectStartupActivity
import com.android.tools.idea.startup.GradleSpecificInitializer
import com.intellij.openapi.actionSystem.impl.ActionConfigurationCustomizer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.impl.ExtensionPointImpl
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import org.hamcrest.CoreMatchers
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Assert.assertThat

class AndroidCourseGeneratorTest : CourseGenerationTestBase<JdkProjectSettings>() {

  override val defaultSettings: JdkProjectSettings get() = JdkProjectSettings.emptySettings()

  override fun setUp() {
    super.setUp()
    disableUnnecessaryExtensions()
  }

  // Disables some extensions provided by AS.
  // They try to set up JAVA and Android JDK in test that we don't need in these tests.
  // So let's unregister them. Otherwise, tests fail
  private fun disableUnnecessaryExtensions() {
    val extensionArea = ApplicationManager.getApplication().extensionArea

    extensionArea
      .getExtensionPoint<ActionConfigurationCustomizer>("com.intellij.actionConfigurationCustomizer")
      .unregisterExtensionInTest(GradleSpecificInitializer::class.java)

    extensionArea.getExtensionPoint(StartupActivity.POST_STARTUP_ACTIVITY)
      .unregisterExtensionInTest(AndroidGradleProjectStartupActivity::class.java)
  }

  @Suppress("UnstableApiUsage")
  private fun <T : Any, K : T> ExtensionPoint<T>.unregisterExtensionInTest(extensionClass: Class<K>) {
    require(this is ExtensionPointImpl)
    val filteredExtensions = extensionList.filter { !extensionClass.isInstance(it) }
    maskAll(filteredExtensions, testRootDisposable, false)
  }

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
