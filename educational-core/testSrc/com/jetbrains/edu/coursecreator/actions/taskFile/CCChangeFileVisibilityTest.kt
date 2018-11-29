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
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class CCChangeFileVisibilityTest : EduActionTestCase() {

  fun `test single task file`() = doAvailableTest(false, "TaskFile1.kt", pathPrefix = "lesson1/task1")
  fun `test single additional file`() = doAvailableTest(false, "TaskFile3.kt", pathPrefix = "lesson1/task1")
  fun `test multiple files with same visibility`() = doAvailableTest(false, "TaskFile1.kt", "TaskFile3.kt", pathPrefix = "lesson1/task1")
  fun `test directory 1`() = doAvailableTest(true, "lesson1/task1/folder1")
  fun `test directory 2`() = doAvailableTest(true, "lesson1/task1/folder2")
  fun `test save stepik change status after undo`() =
    doAvailableTest(false, "TaskFile1.kt", pathPrefix = "lesson1/task1", status = StepikChangeStatus.INFO_AND_CONTENT)
  fun `test in student mode`() = doUnavailableTest("TaskFile1.kt", pathPrefix = "lesson1/task1", courseMode = EduNames.STUDY)
  fun `test multiple files with different visibility`() = doUnavailableTest("TaskFile1.kt", "TaskFile2.kt", pathPrefix = "lesson1/task1")
  fun `test file outside of task`() = doUnavailableTest("lesson1")

  private fun doAvailableTest(
    shouldOppositeActionBeEnabled: Boolean,
    vararg paths: String,
    pathPrefix: String = "",
    status: StepikChangeStatus = StepikChangeStatus.UP_TO_DATE
  ) = doTest(true, shouldOppositeActionBeEnabled, *paths,
             pathPrefix = pathPrefix,
             courseMode = CCUtils.COURSE_MODE,
             status = status)

  private fun doUnavailableTest(
    vararg paths: String,
    courseMode: String = CCUtils.COURSE_MODE,
    pathPrefix: String = ""
  ) = doTest(false, false, *paths, pathPrefix = pathPrefix, courseMode = courseMode, status = StepikChangeStatus.UP_TO_DATE)

  private fun doTest(
    shouldActionBeEnabled: Boolean,
    shouldOppositeActionBeEnabled: Boolean,
    vararg paths: String,
    courseMode: String,
    pathPrefix: String,
    status: StepikChangeStatus
  ) {
    val course = createCourse(courseMode, status)

    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getTaskDir(project) ?: error("Can't find task dir of `${task.name}` task")

    val selectedFiles = mutableListOf<VirtualFile>()
    val affectedCourseFiles = mutableListOf<StudyFile>()

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

    val hide = CCHideFromStudent()
    val show = CCMakeVisibleToStudent()
    checkAction(hide, show, dataContext, affectedCourseFiles, task, shouldActionBeEnabled, shouldOppositeActionBeEnabled)
    affectedCourseFiles.forEach { it.isVisible = !it.isVisible}
    checkAction(show, hide, dataContext, affectedCourseFiles, task, shouldActionBeEnabled, shouldOppositeActionBeEnabled)
  }

  private fun createCourse(
    courseMode: String,
    taskStepikStatus: StepikChangeStatus
  ): Course = courseWithFiles(courseMode = courseMode) {
    lesson("lesson1") {
      eduTask("task1") {
        task.stepId = 1
        task.stepikChangeStatus = taskStepikStatus
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
    affectedCourseFiles: List<StudyFile>,
    task: Task,
    shouldActionBeEnabled: Boolean = true,
    shouldOppositeActionBeEnabled: Boolean = false
  ) {
    val initialStates = affectedCourseFiles.associate { it to it.isVisible }
    val initialPlaceholders = affectedCourseFiles.filterIsInstance<TaskFile>().associate { it to it.answerPlaceholders }
    val stepikInitialStatus = task.stepikChangeStatus

    val oppositeActionPresentation = testAction(dataContext, oppositeAction, false)
    checkActionEnabled(oppositeActionPresentation, shouldOppositeActionBeEnabled)
    val presentation = testAction(dataContext, action)
    checkActionEnabled(presentation, shouldActionBeEnabled)

    if (shouldActionBeEnabled) {
      for (visibleFile in affectedCourseFiles) {
        assertEquals(action.requiredVisibility, visibleFile.isVisible)
        if (!action.requiredVisibility && visibleFile is TaskFile) {
          check(visibleFile.answerPlaceholders.isEmpty()) {
            "Task file `${visibleFile.name}` shouldn't contain placeholders because it's invisible"
          }
        }
      }
      assertEquals(StepikChangeStatus.INFO_AND_CONTENT, task.stepikChangeStatus)
      UndoManager.getInstance(project).undo(null)
      for (visibleFile in affectedCourseFiles) {
        assertEquals(initialStates[visibleFile], visibleFile.isVisible)
        if (visibleFile is TaskFile) {
          assertEquals(initialPlaceholders[visibleFile], visibleFile.answerPlaceholders)
        }
      }
      assertEquals(stepikInitialStatus, task.stepikChangeStatus)
    }
  }
}
