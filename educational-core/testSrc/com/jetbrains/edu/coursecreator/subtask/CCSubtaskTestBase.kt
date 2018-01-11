package com.jetbrains.edu.coursecreator.subtask

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.MapDataContext
import com.jetbrains.edu.coursecreator.CCTestCase
import com.jetbrains.edu.coursecreator.actions.CCNewSubtaskAction
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.SubtaskUtils
import com.jetbrains.edu.learning.courseFormat.TaskFile

abstract class CCSubtaskTestBase : CCTestCase() {

  abstract protected val courseBuilder: EduCourseBuilder<*>
  abstract protected val taskFileName: String
  abstract protected val testFileName: String
  abstract protected val language: Language

  private val srcDirPath: String get() = join(TASK_PATH, courseBuilder.sourceDir)
  private val testDirPath: String get() = join(TASK_PATH, courseBuilder.testDir)

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
    taskFile.name = taskFileName
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

    val testFileAfter = myFixture.findFileInTempDir(testFilePath)
    assertNull("`$testFilePath` should be renamed while subtask creation", testFileAfter)
    checkFileExists("$testDirPath/${SubtaskUtils.getTestFileName(project, 0)}")
    checkFileExists("$testDirPath/${SubtaskUtils.getTestFileName(project, 1)}")
  }

  private fun checkFileExists(filePath: String) {
    myFixture.findFileInTempDir(filePath) ?: error("Failed to find `$filePath`")
  }

  private fun dataContext(file: VirtualFile): DataContext {
    return MapDataContext().apply {
      put(CommonDataKeys.PROJECT, project)
      put(CommonDataKeys.VIRTUAL_FILE, file)
    }
  }

  companion object {
    private const val TASK_PATH: String = "lesson1/task1"

    private fun join(vararg parts: String): String =
      parts.filter { it.isNotEmpty() }.joinToString(VfsUtilCore.VFS_SEPARATOR_CHAR.toString())
  }
}
