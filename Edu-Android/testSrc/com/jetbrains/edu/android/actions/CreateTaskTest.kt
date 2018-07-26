package com.jetbrains.edu.android.actions

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.android.Android
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduTestInputDialog
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.gradle.JdkProjectSettings
import com.jetbrains.edu.learning.withTestDialog
import junit.framework.TestCase

class CreateTaskTest : EduActionTestCase() {

  fun `test create task in empty lesson`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = Android, settings = JdkProjectSettings.emptySettings()) {
      lesson()
    }
    val lessonFile = findFile("lesson1")

    withTestDialog(EduTestInputDialog("task1")) {
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
        }
        file("task.html")
        file("build.gradle")
      }
      file("local.properties")
      file("build.gradle")
      file("settings.gradle")
    }
    expectedFileTree.assertEquals(LightPlatformTestCase.getSourceRoot())
  }
}
