package com.jetbrains.edu.coursecreator.taskDescription

import com.intellij.xml.util.CheckDtdReferencesInspection
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.taskDescription.toShortcut

class ShortcutsHighlightingTest : EduTestCase() {
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(listOf(CheckDtdReferencesInspection::class.java))
  }

  fun `test unresolved shortcut in html not highlighted`() {
    doTest(DescriptionFormat.HTML)
  }

  fun `test unresolved shortcut in md not highlighted`() {
    doTest(DescriptionFormat.MD)
  }

  private fun doTest(descriptionFormat: DescriptionFormat = DescriptionFormat.HTML) {
    val taskDescriptionText = "Text with shortcut: ${"CodeCompletion".toShortcut()}"
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask(taskDescriptionFormat = descriptionFormat, taskDescription = taskDescriptionText) {
          taskFile("taskFile1.txt")
        }
      }
    }

    val task = findTask(0, 0)
    val taskDescriptionFile = task.getDir(project)?.findChild(descriptionFormat.descriptionFileName) ?: error("No task description file")

    myFixture.openFileInEditor(taskDescriptionFile)
    myFixture.checkHighlighting()
  }
}