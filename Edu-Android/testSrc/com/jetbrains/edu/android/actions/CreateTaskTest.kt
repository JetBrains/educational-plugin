package com.jetbrains.edu.android.actions

import com.intellij.lang.Language
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.fileTree
import junit.framework.TestCase

class CreateTaskTest : EduActionTestCase() {
  private object FakeKotlin : Language("kotlin")

  fun `test create task in empty lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeKotlin,
                                 environment = EduNames.ANDROID, settings = JdkProjectSettings.emptySettings()) {
      lesson()
    }
    val lessonFile = findFile("lesson1")

    withMockCreateStudyItemUi(MockAndroidNewStudyUi("task1", "com.edu.task1")) {
      testAction(dataContext(lessonFile), CCCreateTask())
    }
    TestCase.assertEquals(1, course.lessons[0].taskList.size)
    val expectedFileTree = fileTree {
      dir("lesson1/task1") {
        dir("src") {
          dir("main") {
            dir("java/com/edu/task1") {
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
          dir("test/java/com/edu/task1") {
            file("ExampleUnitTest.kt")
          }
          dir("androidTest/java/com/edu/task1") {
            file("AndroidEduTestRunner.kt")
          }
        }
        file("task.html")
        file("build.gradle")
      }
      file("local.properties")
      file("gradle.properties")
      file("build.gradle")
      file("settings.gradle")
    }
    expectedFileTree.assertEquals(LightPlatformTestCase.getSourceRoot())
  }
}
