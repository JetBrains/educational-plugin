package com.jetbrains.edu.cpp.inspections

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.BuildNumber
import com.intellij.testFramework.EditorTestUtil
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.cpp.messages.EduCppBundle
import com.jetbrains.edu.learning.EduTestCase

class NoTaskProjectNameInspectionTest : EduTestCase() {

  // BACKCOMPAT: 2019.1. We skip tests in build 191 because of file inspection doesn't work correctly in test mode.
  override fun runTest() {
    val build192: BuildNumber = BuildNumber.fromString("192")
    if (ApplicationInfo.getInstance().build >= build192) {
      super.runTest()
    }
  }

  private fun addDeletedTaskProjectNameToCMakeListsTestBase(beforeWithHighlighting: String, after: String) {
    courseWithFiles(
      language = OCLanguage.getInstance(),
      courseMode = CCUtils.COURSE_MODE,
      environment = "GoogleTest" // Environment doesn't matter here
    ) {
      lesson("lesson") {
        eduTask("task") {
          taskFile(CMakeListsFileType.FILE_NAME, beforeWithHighlighting)
        }
      }
    }

    doTest(EduCppBundle.message("project.name.not.set.fix.description"), after)
  }

  fun `test add deleted project name to CMakeList with set minimum required`() =
    addDeletedTaskProjectNameToCMakeListsTestBase(
      """
        |<warning descr="Project name isn't set. It could break project structure.">cmake_minimum_required(VERSION 3.15)
        |# some text<caret></warning>
      """.trimMargin(),
      """
        |cmake_minimum_required(VERSION 3.15)
        |project(global-lesson-task)
        |# some text
      """.trimMargin()
    )

  fun `test add deleted project name to CMakeList without minimum required`() =
    addDeletedTaskProjectNameToCMakeListsTestBase(
      """<warning descr="Project name isn't set. It could break project structure."># some text<caret></warning>""",
      """
        |project(global-lesson-task)
        |# some text
      """.trimMargin()
    )

  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(listOf(NoTaskProjectNameInspection::class.java))
  }

  private fun doTest(quickFixName: String, expectedText: String) {
    withMockCreateStudyItemUi(MockNewStudyItemUi()) {
      runTestHighlighting()
      val action = myFixture.findSingleIntention(quickFixName)
      myFixture.launchAction(action)
      myFixture.checkResult(expectedText)
    }
  }

  private fun runTestHighlighting() {
    val cMakeListsFile = findFile("lesson/task/${CMakeListsFileType.FILE_NAME}")

    val document = FileDocumentManager.getInstance().getDocument(cMakeListsFile)
    kotlin.test.assertNotNull(document, "CMakeLists file document is null")

    myFixture.openFileInEditor(cMakeListsFile)

    val caretsState = EditorTestUtil.extractCaretAndSelectionMarkers(document)
    EditorTestUtil.setCaretsAndSelection(myFixture.editor, caretsState)

    myFixture.checkHighlighting()
  }
}