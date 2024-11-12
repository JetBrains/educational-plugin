package com.jetbrains.edu.android.courseGeneration

import com.android.tools.idea.gradle.project.AndroidGradleProjectStartupActivity
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.impl.ExtensionPointImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.framework.impl.visitFrameworkLessons
import com.jetbrains.edu.learning.gradle.GradleConstants.BUILD_GRADLE
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class AndroidCourseGeneratorTest : JvmCourseGenerationTestBase() {

  override fun setUp() {
    super.setUp()

    // Disables some extensions provided by AS.
    // They try to set up JAVA and Android JDK, or run Gradle import in tests where we don't need it.
    // So let's unregister them. Otherwise, tests fail
    ApplicationManager.getApplication().extensionArea
      .getExtensionPoint<ProjectActivity>("com.intellij.postStartupActivity")
      // Use `unregisterExtensionInTest` instead when we migrate our startup activities to `ProjectActivity` API
      .unregisterExtension(AndroidGradleProjectStartupActivity::class.java)
    disableProjectSyncNotifications()
  }

  @Test
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
                file("colors.xml")
                file("strings.xml")
                file("themes.xml")
              }
              dir("values-night") {
                file("themes.xml")
              }
            }
            file("AndroidManifest.xml")
          }
          dir("test/java/com/example/android/course") {
            file("ExampleUnitTest.kt")
          }
          dir("androidTest/java/com/example/android/course") {
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

  @Test
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

  @Test
  fun `test migrate to new test runner in educator mode`() {
    val course = oldAndroidCourse(CourseMode.EDUCATOR)
    createCourseStructure(course)

    fileTree {
      dir("lesson1") {
        dir("task1") {
          file("task.md")
          file(BUILD_GRADLE, appBuildGradleWithDefaultTestRunner())
          dir("src/androidTest/java/foo/bar") {
            file("ExampleInstrumentedTest.kt")
          }
        }
        dir("task2") {
          file("task.md")
          file(BUILD_GRADLE, appBuildGradleWithDefaultTestRunner())
          dir("src/androidTest/java/foo/bar") {
            file("ExampleInstrumentedTest.kt")
          }
        }
      }
      dir("lesson2/task3") {
        file("task.md")
        file(BUILD_GRADLE, appBuildGradleWithDefaultTestRunner())
        dir("src/androidTest/java/foo/bar") {
          file("ExampleInstrumentedTest.kt")
        }
      }
      dir("lesson3/task4") {
        file("task.md")
        file(BUILD_GRADLE, appBuildGradleWithDefaultTestRunner())
        dir("src/androidTest/java/foo/bar") {
          file("ExampleInstrumentedTest.kt")
        }
      }
      gradleWrapperFiles()
      file("local.properties")
      file("gradle.properties")
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }

  @Test
  fun `test migrate to new test runner in student mode`() {
    val course = oldAndroidCourse(CourseMode.STUDENT)
    createCourseStructure(course)

    FileDocumentManager.getInstance().saveAllDocuments()

    fileTree {
      dir("lesson1") {
        dir("task") {
          file(BUILD_GRADLE, appBuildGradleWithDefaultTestRunner())
          dir("src/androidTest/java/foo/bar") {
            file("ExampleInstrumentedTest.kt")
          }
        }
        dir("task1") {
          file("task.md")
        }
        dir("task2") {
          file("task.md")
        }
      }
      dir("lesson2/task3") {
        file("task.md")
        file(BUILD_GRADLE, appBuildGradleWithDefaultTestRunner())
        dir("src/androidTest/java/foo/bar") {
          file("ExampleInstrumentedTest.kt")
        }
      }
      dir("lesson3/task4") {
        file("task.md")
        file(BUILD_GRADLE, appBuildGradleWithDefaultTestRunner())
        dir("src/androidTest/java/foo/bar") {
          file("ExampleInstrumentedTest.kt")
        }
      }
      gradleWrapperFiles()
      file("local.properties")
      file("gradle.properties")
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)


    val flm = FrameworkLessonManager.getInstance(project)
    course.visitFrameworkLessons { lesson ->
      for (task in lesson.taskList) {
        if (task != lesson.currentTask()) {
          val state = flm.getTaskState(lesson, task)
          assertEquals("$BUILD_GRADLE in ${task.name} is not updated", appBuildGradleWithDefaultTestRunner(), state[BUILD_GRADLE])
          assertNull("${task.name} shouldn't contains old `AndroidEduTestRunner.kt`", state["src/androidTest/java/foo/bar/AndroidEduTestRunner.kt"])
        }
      }
    }
  }

  private fun FileTreeBuilder.gradleWrapperFiles() {
    dir("gradle/wrapper") {
      file("gradle-wrapper.jar")
      file("gradle-wrapper.properties")
    }
    file("gradlew")
    file("gradlew.bat")
  }

  // Doesn't allow `ProjectSyncStatusNotificationProvider` to show `ProjectSyncStatusNotificationProvider.ProjectStructureNotificationPanel`.
  // These tests are not intended to check Gradle sync, only file structure
  private fun disableProjectSyncNotifications() {
    val component = PropertiesComponent.getInstance()
    val oldValue = component.getValue(PROJECT_STRUCTURE_NOTIFICATION_LAST_HIDDEN_TIMESTAMP)
    component.setValue(PROJECT_STRUCTURE_NOTIFICATION_LAST_HIDDEN_TIMESTAMP, System.currentTimeMillis().toString())
    Disposer.register(testRootDisposable) {
      PropertiesComponent.getInstance().setValue(PROJECT_STRUCTURE_NOTIFICATION_LAST_HIDDEN_TIMESTAMP, oldValue)
    }
  }

  companion object {
    // See com.android.tools.idea.gradle.notification.ProjectSyncStatusNotificationProvider.ProjectStructureNotificationPanel.userAllowsShow
    private const val PROJECT_STRUCTURE_NOTIFICATION_LAST_HIDDEN_TIMESTAMP = "PROJECT_STRUCTURE_NOTIFICATION_LAST_HIDDEN_TIMESTAMP"

    private fun oldAndroidCourse(courseMode: CourseMode): Course {
      return course(language = KotlinLanguage.INSTANCE, environment = EduNames.ANDROID, courseMode = courseMode) {
        frameworkLesson("lesson1") {
          eduTask("task1") {
            taskFile(BUILD_GRADLE, appBuildGradleWithOldTestRunner())
            taskFile("src/androidTest/java/foo/bar/AndroidEduTestRunner.kt")
            taskFile("src/androidTest/java/foo/bar/ExampleInstrumentedTest.kt")
          }
          eduTask("task2") {
            taskFile(BUILD_GRADLE, appBuildGradleWithOldTestRunner())
            taskFile("src/androidTest/java/foo/bar/AndroidEduTestRunner.kt")
            taskFile("src/androidTest/java/foo/bar/ExampleInstrumentedTest.kt")
          }
        }
        lesson("lesson2") {
          eduTask("task3") {
            taskFile(BUILD_GRADLE, appBuildGradleWithOldTestRunner())
            taskFile("src/androidTest/java/foo/bar/AndroidEduTestRunner.kt")
            taskFile("src/androidTest/java/foo/bar/ExampleInstrumentedTest.kt")
          }
        }
        lesson("lesson3") {
          eduTask("task4") {
            taskFile(BUILD_GRADLE, appBuildGradleWithDefaultTestRunner())
            taskFile("src/androidTest/java/foo/bar/ExampleInstrumentedTest.kt")
          }
        }
      }
    }

    private fun appBuildGradleWithOldTestRunner(): String = appBuildGradle("foo.bar.AndroidEduTestRunner")

    private fun appBuildGradleWithDefaultTestRunner(): String = appBuildGradle("androidx.test.runner.AndroidJUnitRunner")

    private fun appBuildGradle(runnerClass: String): String {
      return """
        plugins {
            id 'com.android.application'
            id 'org.jetbrains.kotlin.android'
        }
        
        android {
            namespace 'foo.bar'
            compileSdk 30
        
            defaultConfig {
                applicationId "foo.bar"
                minSdk 30
                targetSdk 30
                versionCode 1
                versionName "1.0"
        
                testInstrumentationRunner "$runnerClass"
            }
        
            buildTypes {
                release {
                    minifyEnabled false
                    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
                }
            }
            compileOptions {
                sourceCompatibility JavaVersion.VERSION_1_8
                targetCompatibility JavaVersion.VERSION_1_8
            }
            kotlinOptions {
                jvmTarget = '1.8'
            }
        }            
      """.trimIndent()
    }

    @Suppress("UnstableApiUsage")
    fun <T : Any, K : T> ExtensionPoint<T>.unregisterExtensionInTest(extensionClass: Class<K>, disposable: Disposable) {
      require(this is ExtensionPointImpl)
      val filteredExtensions = extensionList.filter { !extensionClass.isInstance(it) }
      maskAll(filteredExtensions, disposable, false)
    }
  }
}
