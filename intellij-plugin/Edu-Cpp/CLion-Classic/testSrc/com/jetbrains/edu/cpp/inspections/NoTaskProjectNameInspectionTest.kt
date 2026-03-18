package com.jetbrains.edu.cpp.inspections

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.testFramework.EditorTestUtil
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.cpp.messages.EduCppBundle
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.junit.Test

class NoTaskProjectNameInspectionTest : EduTestCase() {

  @Test
  fun `test add deleted project name to CMakeList with set minimum required`() {
    courseWithFiles(
      language = OCLanguage.getInstance(),
      courseMode = CourseMode.EDUCATOR,
      environment = "GoogleTest" // Environment doesn't matter here
    ) {
      lesson("lesson") {
        eduTask("task") {
          taskFile(CMakeListsFileType.FILE_NAME, """
          |<warning descr="${EduCppBundle.message("project.name.not.set.warning")}">cmake_minimum_required(VERSION 3.15)
          |# some text<caret></warning>
        """.trimMargin())
        }
      }
    }
    doTest("lesson/task", """
      |cmake_minimum_required(VERSION 3.15)
      |project(global-lesson-task)
      |# some text
    """.trimMargin())
  }

  @Test
  fun `test add deleted project name to CMakeList without minimum required`() {
    courseWithFiles(
      language = OCLanguage.getInstance(),
      courseMode = CourseMode.EDUCATOR,
      environment = "GoogleTest" // Environment doesn't matter here
    ) {
      lesson("lesson") {
        eduTask("task") {
          taskFile(CMakeListsFileType.FILE_NAME,
                   """<warning descr="${EduCppBundle.message("project.name.not.set.warning")}"># some text<caret></warning>""")
        }
      }
    }
    doTest("lesson/task", """
      |project(global-lesson-task)
      |# some text
    """.trimMargin())
  }

  @Test
  fun `test add deleted project name to CMakeList with custom task name`() {
    courseWithFiles(
      language = OCLanguage.getInstance(),
      courseMode = CourseMode.EDUCATOR,
      environment = "GoogleTest" // Environment doesn't matter here
    ) {
      lesson("lesson") {
        eduTask("custom task name") {
          taskFile(CMakeListsFileType.FILE_NAME, """
          |<warning descr="${EduCppBundle.message("project.name.not.set.warning")}">cmake_minimum_required(VERSION 3.15)
          |# some text<caret></warning>
        """.trimMargin())
        }
      }
    }
    doTest("lesson/custom task name", """
      |cmake_minimum_required(VERSION 3.15)
      |project(global-lesson-custom_task_name)
      |# some text
    """.trimMargin())
  }

  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(listOf(NoTaskProjectNameInspection::class.java))
  }

  private fun doTest(taskPath: String, expectedText: String) {
    withMockCreateStudyItemUi(MockNewStudyItemUi()) {
      runTestHighlighting(taskPath)
      val action = myFixture.findSingleIntention(EduCppBundle.message("project.name.not.set.fix.description"))
      myFixture.launchAction(action)
      myFixture.checkResult(expectedText)
    }
  }

  private fun runTestHighlighting(taskPath: String) {
    val cMakeListsFile = findFile("$taskPath/${CMakeListsFileType.FILE_NAME}")

    val document = FileDocumentManager.getInstance().getDocument(cMakeListsFile)
    checkNotNull(document) { "CMakeLists file document is null" }

    myFixture.openFileInEditor(cMakeListsFile)

    val caretsState = EditorTestUtil.extractCaretAndSelectionMarkers(document)
    EditorTestUtil.setCaretsAndSelection(myFixture.editor, caretsState)

    myFixture.checkHighlighting()
  }
}