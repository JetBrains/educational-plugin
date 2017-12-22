package com.jetbrains.edu.coursecreator.subtask

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.MapDataContext
import com.jetbrains.edu.coursecreator.CCTestCase
import com.jetbrains.edu.coursecreator.actions.CCNewSubtaskAction
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.SubtaskUtils
import com.jetbrains.edu.learning.courseFormat.TaskFile

abstract class CCSubtaskTestBase : CCTestCase() {

  abstract protected val srcDirPath: String
  abstract protected val testDirPath: String
  abstract protected val taskFileName: String
  abstract protected val testFileName: String
  abstract protected val language: Language

  override fun setUp() {
    super.setUp()
    runWriteAction {
      val src = VfsUtil.createDirectoryIfMissing(myFixture.findFileInTempDir("."), srcDirPath)
      val test = VfsUtil.createDirectoryIfMissing(myFixture.findFileInTempDir("."), testDirPath)
      src.createChildData(this, taskFileName)
      test.createChildData(this, testFileName)
    }

    val course = StudyTaskManager.getInstance(project).course ?: error("Failed to find current course")
    course.language = language.id
    val task = course.lessons[0].taskList[0]
    val taskFile = TaskFile()
    taskFile.task = task
    task.taskFiles[taskFileName] = taskFile
    task.testsText[testFileName] = ""
  }

  fun `test create subtask`() {
    val testFilePath = "$testDirPath/$testFileName"
    val testFile = myFixture.findFileInTempDir(testFilePath) ?: error("Failed to find `$testFilePath`")
    myFixture.configureFromExistingVirtualFile(testFile)

    val presentation = testAction(dataContext(testFile), CCNewSubtaskAction())
    assertTrue("${CCNewSubtaskAction::class.simpleName} should be enabled and visible", presentation.isEnabledAndVisible)

    val expectedSubtaskPath = "$testDirPath/${SubtaskUtils.getTestFileName(project, 1)}"
    myFixture.findFileInTempDir(expectedSubtaskPath) ?: error("Failed to find `$expectedSubtaskPath`")
  }

  private fun dataContext(file: VirtualFile): DataContext {
    return MapDataContext().apply {
      put(CommonDataKeys.PROJECT, project)
      put(CommonDataKeys.VIRTUAL_FILE, file)
    }
  }
}
