package com.jetbrains.edu.android.actions

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateTask
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.testAction
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class AndroidCreateTaskTest : EduActionTestCase() {
  @Test
  fun `test create task in empty lesson`() {
    val course = courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      environment = EduNames.ANDROID,
      language = KotlinLanguage.INSTANCE
    ) {
      lesson()
    }
    val lessonFile = findFile("lesson1")

    withMockCreateStudyItemUi(MockAndroidNewStudyUi("task1", "com.edu.task1")) {
      testAction(CCCreateTask.ACTION_ID, dataContext(lessonFile))
    }
    assertEquals(1, course.lessons[0].taskList.size)
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
          dir("test/java/com/edu/task1") {
            file("ExampleUnitTest.kt")
          }
          dir("androidTest/java/com/edu/task1") {
            file("ExampleInstrumentedTest.kt")
          }
        }
        file("task.md")
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
