package com.jetbrains.edu.learning

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillCourseGenerationTest : EduTestCase() {
  fun `test course structure creation`() {
    courseWithFiles(courseProducer = ::HyperskillCourse, courseMode = CCUtils.COURSE_MODE) {}

    checkFileTree {
      dir("lesson1/task1") {
        dir("tests") {
          file("Tests.txt")
        }
        file("Task.txt")
        file("task.html")
      }
    }
  }

  private fun checkFileTree(block: FileTreeBuilder.() -> Unit) {
    fileTree(block).assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }
}