package com.jetbrains.edu.learning

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillCourseGenerationTest : EduTestCase() {
  fun `test course structure creation`() {
    val useHtml = CCSettings.getInstance().useHtmlAsDefaultTaskFormat()
    try {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(true)

      courseWithFiles(courseProducer = ::HyperskillCourse, courseMode = CCUtils.COURSE_MODE) {}
    }
    finally {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(useHtml)
    }
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