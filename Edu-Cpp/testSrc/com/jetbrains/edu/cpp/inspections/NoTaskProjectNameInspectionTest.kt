package com.jetbrains.edu.cpp.inspections

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.testFramework.EditorTestUtil
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.cpp.messages.EduCppBundle
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.ext.findSourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class NoTaskProjectNameInspectionTest : EduTestCase() {

  fun `test add deleted project name to CMakeList with set minimum required`() {
    val course = courseWithFiles(
      language = OCLanguage.getInstance(),
      courseMode = CCUtils.COURSE_MODE,
      environment = "GoogleTest"
    ) {
      lesson("lesson") {
        eduTask("task") {
          taskFile("src/${CMakeListsFileType.FILE_NAME}", """
            |<warning descr="Project name isn't set. It could break project structure.">cmake_minimum_required(VERSION 3.15)
            |# some text</warning>
            """.trimMargin("|"))
        }
      }
    }

    val task = course.findTask("lesson", "task")
    doTest(task, EduCppBundle.message("projectName.addDefault.fix.description"), """
      |cmake_minimum_required(VERSION 3.15)
      |project(global-lesson-task)
      |# some text
    """.trimMargin("|"))
  }

  fun `test add deleted project name to CMakeList without minimum required`() {
    val course = courseWithFiles(
      language = OCLanguage.getInstance(),
      courseMode = CCUtils.COURSE_MODE,
      environment = "GoogleTest"
    ) {
      lesson("lesson") {
        eduTask("task") {
          taskFile("src/${CMakeListsFileType.FILE_NAME}",
                   """<warning descr="Project name isn't set. It could break project structure."># some text</warning>""")
        }
      }
    }

    val task = course.findTask("lesson", "task")
    doTest(task, EduCppBundle.message("projectName.addDefault.fix.description"), """
      |project(global-lesson-task)
      |# some text
    """.trimMargin("|"))
  }

  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(listOf(NoTaskProjectNameInspection::class.java))
  }

  private fun doTest(task: Task, quickFixName: String, expectedText: String) {
    withMockCreateStudyItemUi(MockNewStudyItemUi()) {
      testHighlighting(task)
      val action = myFixture.findSingleIntention(quickFixName)
      myFixture.launchAction(action)
      myFixture.checkResult(expectedText)
    }
  }

  private fun testHighlighting(task: Task) {
    val configFile = runWriteAction {
      val taskDir = task.getTaskDir(project)!!
      task.findSourceDir(taskDir)!!.findOrCreateChildData(project, CMakeListsFileType.FILE_NAME)
    }
    val document = FileDocumentManager.getInstance().getDocument(configFile)!!
    myFixture.openFileInEditor(configFile)
    val caretsState = EditorTestUtil.extractCaretAndSelectionMarkers(document)
    EditorTestUtil.setCaretsAndSelection(myFixture.editor, caretsState)

    myFixture.checkHighlighting()
  }
}