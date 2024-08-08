package com.jetbrains.edu.coursecreator.taskDescription

import com.intellij.xml.util.CheckDtdReferencesInspection
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.taskToolWindow.toShortcut
import org.junit.Test

class ShortcutsHighlightingTest : EduTestCase() {
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(listOf(CheckDtdReferencesInspection::class.java))
  }

  @Test
  fun `test unresolved shortcut in html not highlighted`() {
    doTest(DescriptionFormat.HTML)
  }

  @Test
  fun `test unresolved shortcut in md not highlighted`() {
    doTest(DescriptionFormat.MD)
  }

  private fun doTest(descriptionFormat: DescriptionFormat = DescriptionFormat.HTML) {
    val taskDescriptionText = "Text with shortcut: ${"CodeCompletion".toShortcut()}"
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask(taskDescriptionFormat = descriptionFormat, taskDescription = taskDescriptionText) {
          taskFile("taskFile1.txt")
        }
      }
    }

    val task = findTask(0, 0)
    val taskDescriptionFile = task.getDir(project.courseDir)?.findChild(descriptionFormat.fileName) ?: error("No task description file")

    myFixture.openFileInEditor(taskDescriptionFile)
    myFixture.checkHighlighting()
  }
}