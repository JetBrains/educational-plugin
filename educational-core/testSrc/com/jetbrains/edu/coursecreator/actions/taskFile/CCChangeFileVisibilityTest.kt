package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile

class CCChangeFileVisibilityTest : EduActionTestCase() {

  fun `test single task file`() = doAvailableTest(false, "TaskFile1.kt", pathPrefix = "lesson1/task1")
  fun `test single additional file`() = doAvailableTest(false, "TaskFile3.kt", pathPrefix = "lesson1/task1")
  fun `test multiple files with same visibility`() = doAvailableTest(false, "TaskFile1.kt", "TaskFile3.kt", pathPrefix = "lesson1/task1")
  fun `test directory 1`() = doAvailableTest(true, "lesson1/task1/folder1")
  fun `test directory 2`() = doAvailableTest(true, "lesson1/task1/folder2")
  fun `test save stepik change status after undo`() =
    doAvailableTest(false, "TaskFile1.kt", pathPrefix = "lesson1/task1")
  fun `test in student mode`() = doUnavailableTest("TaskFile1.kt", pathPrefix = "lesson1/task1", courseMode = EduNames.STUDY)
  fun `test multiple files with different visibility`() = doUnavailableTest("TaskFile1.kt", "TaskFile2.kt", pathPrefix = "lesson1/task1")
  fun `test file outside of task`() = doUnavailableTest("lesson1")

  private fun doAvailableTest(
    shouldOppositeActionBeEnabled: Boolean,
    vararg paths: String,
    pathPrefix: String = ""
  ) = doTest(true, shouldOppositeActionBeEnabled, *paths,
             courseMode = CCUtils.COURSE_MODE,
             pathPrefix = pathPrefix)

  private fun doUnavailableTest(
    vararg paths: String,
    courseMode: String = CCUtils.COURSE_MODE,
    pathPrefix: String = ""
  ) = doTest(false, false, *paths, courseMode = courseMode, pathPrefix = pathPrefix)

  private fun doTest(
    shouldActionBeEnabled: Boolean,
    shouldOppositeActionBeEnabled: Boolean,
    vararg paths: String,
    courseMode: String,
    pathPrefix: String
  ) {
    val course = createCourse(courseMode)

    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getTaskDir(project) ?: error("Can't find task dir of `${task.name}` task")

    val selectedFiles = mutableListOf<VirtualFile>()
    val affectedCourseFiles = mutableListOf<TaskFile>()

    for (path in paths) {
      val fullPath = if (pathPrefix.isEmpty()) path else "$pathPrefix/$path"
      val file = LightPlatformTestCase.getSourceRoot().findFileByRelativePath(fullPath) ?: error("Can't find `$path` file")
      selectedFiles += file

      if (VfsUtil.isAncestor(taskDir, file, true)) {
        if (file.isDirectory) {
          affectedCourseFiles += VfsUtil.collectChildrenRecursively(file).mapNotNull {
            if (it.isDirectory) return@mapNotNull null
            val pathInTask = FileUtil.getRelativePath(taskDir.path, it.path, VfsUtilCore.VFS_SEPARATOR_CHAR)!!
            task.getTaskFile(pathInTask)
          }
        } else {
          val visibleFile = task.getTaskFile(path) ?: error("Can't find `$fullPath` in course")
          affectedCourseFiles += visibleFile
        }
      }
    }


    val dataContext = if (selectedFiles.size == 1) {
      dataContext(selectedFiles.single())
    } else {
      dataContext(selectedFiles.toTypedArray())
    }

    val hide = CCHideFromLearner()
    val show = CCMakeVisibleToLearner()
    checkAction(hide, show, dataContext, affectedCourseFiles, shouldActionBeEnabled, shouldOppositeActionBeEnabled)
    affectedCourseFiles.forEach { it.isVisible = !it.isVisible}
    checkAction(show, hide, dataContext, affectedCourseFiles, shouldActionBeEnabled, shouldOppositeActionBeEnabled)
  }

  private fun createCourse(
    courseMode: String
  ): Course = courseWithFiles(courseMode = courseMode) {
    lesson("lesson1") {
      eduTask("task1") {
        task.id = 1
        taskFile("TaskFile1.kt", "<p>some text</p>")
        taskFile("TaskFile2.kt", visible = false)
        taskFile("TaskFile3.kt")
        dir("folder1") {
          taskFile("TaskFile4.kt")
          taskFile("TaskFile5.kt")
        }
        dir("folder2") {
          taskFile("TaskFile6.kt", visible = false)
          taskFile("TaskFile7.kt")
        }
      }
    }
  }

  private fun checkAction(
    action: CCChangeFileVisibility,
    oppositeAction: CCChangeFileVisibility,
    dataContext: DataContext,
    affectedCourseFiles: List<TaskFile>,
    shouldActionBeEnabled: Boolean = true,
    shouldOppositeActionBeEnabled: Boolean = false
  ) {
    val initialStates = affectedCourseFiles.associate { it to it.isVisible }
    val initialPlaceholders = affectedCourseFiles.associate { it to it.answerPlaceholders }

    val oppositeActionPresentation = testAction(dataContext, oppositeAction, false)
    checkActionEnabled(oppositeActionPresentation, shouldOppositeActionBeEnabled)
    val presentation = testAction(dataContext, action)
    checkActionEnabled(presentation, shouldActionBeEnabled)

    if (shouldActionBeEnabled) {
      for (visibleFile in affectedCourseFiles) {
        assertEquals(action.requiredVisibility, visibleFile.isVisible)
        if (!action.requiredVisibility) {
          check(visibleFile.answerPlaceholders.isEmpty()) {
            "Task file `${visibleFile.name}` shouldn't contain placeholders because it's invisible"
          }
        }
      }
      UndoManager.getInstance(project).undo(null)
      for (visibleFile in affectedCourseFiles) {
        assertEquals(initialStates[visibleFile], visibleFile.isVisible)
        assertEquals(initialPlaceholders[visibleFile], visibleFile.answerPlaceholders)
      }
    }
  }
}
