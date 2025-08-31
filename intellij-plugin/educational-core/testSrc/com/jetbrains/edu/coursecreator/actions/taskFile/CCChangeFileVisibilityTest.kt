package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getAdditionalFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import org.junit.Test
import kotlin.test.assertContentEquals

class CCChangeFileVisibilityTest : EduActionTestCase() {

  @Test
  fun `test single task file`() = doAvailableTest(false, "TaskFile1.kt", pathPrefix = "lesson1/task1")
  @Test
  fun `test single task file 2`() = doAvailableTest(false, "TaskFile3.kt", pathPrefix = "lesson1/task1")
  @Test
  fun `test multiple files with same visibility`() = doAvailableTest(false, "TaskFile1.kt", "TaskFile3.kt", pathPrefix = "lesson1/task1")
  @Test
  fun `test directory 1`() = doAvailableTest(true, "lesson1/task1/folder1")
  @Test
  fun `test directory 2`() = doAvailableTest(true, "lesson1/task1/folder2")
  @Test
  fun `test in student mode`() = doUnavailableTest("TaskFile1.kt", pathPrefix = "lesson1/task1", courseMode = CourseMode.STUDENT)
  @Test
  fun `test multiple files with different visibility`() = doUnavailableTest("TaskFile1.kt", "TaskFile2.kt", pathPrefix = "lesson1/task1")

  @Test
  fun `test single invisible additional file`() = doOppositeAvailableTest("a.txt")
  @Test
  fun `test single visible additional file`() = doAvailableTest(false, "a-visible.txt")
  @Test
  fun `test several invisible additional files`() = doOppositeAvailableTest("a.txt", "dir/x.txt")
  @Test
  fun `test several visible additional files`() = doAvailableTest(false, "a-visible.txt", "dir/x-visible.txt")
  @Test
  fun `test additional files with different visibility`() = doUnavailableTest("a.txt", "a-visible.txt")

  @Test
  fun `test visible task and additional files simultaneously`() = doAvailableTest(false, "a-visible.txt", "lesson1/task1/TaskFile1.kt")
  @Test
  fun `test invisible task and additional files simultaneously`() = doOppositeAvailableTest("a.txt", "lesson1/task1/TaskFile2.kt")
  @Test
  fun `test available for task folder`() = doAvailableTest(true, "lesson1/task1")
  @Test
  fun `test available for folder containing tasks`() = doAvailableTest(true, "lesson1", "dir")

  private fun doAvailableTest(
    shouldOppositeActionBeEnabled: Boolean,
    vararg paths: String,
    pathPrefix: String = ""
  ) = doTest(shouldActionBeEnabled = true, shouldOppositeActionBeEnabled, *paths, courseMode = CourseMode.EDUCATOR, pathPrefix = pathPrefix)

  private fun doUnavailableTest(
    vararg paths: String,
    courseMode: CourseMode = CourseMode.EDUCATOR,
    pathPrefix: String = ""
  ) = doTest(shouldActionBeEnabled = false, shouldOppositeActionBeEnabled = false, *paths, courseMode = courseMode, pathPrefix = pathPrefix)

  private fun doOppositeAvailableTest(
    vararg paths: String,
    courseMode: CourseMode = CourseMode.EDUCATOR,
    pathPrefix: String = ""
  ) = doTest(shouldActionBeEnabled = false, shouldOppositeActionBeEnabled = true, *paths, courseMode = courseMode, pathPrefix = pathPrefix)

  private fun doTest(
    shouldActionBeEnabled: Boolean,
    shouldOppositeActionBeEnabled: Boolean,
    vararg paths: String,
    courseMode: CourseMode,
    pathPrefix: String
  ) {
    val course = createCourse(courseMode)

    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getDir(project.courseDir) ?: error("Can't find task dir of `${task.name}` task")

    val selectedFiles = mutableListOf<VirtualFile>()
    val affectedCourseFiles = mutableListOf<EduFile>()

    fun VirtualFile.toEduFile(): EduFile? {
      val pathIsInsideTask = VfsUtil.isAncestor(taskDir, this, true)
      return if (pathIsInsideTask) {
        val taskPath = pathRelativeToTask(project)
        task.getTaskFile(taskPath)
      }
      else {
        val coursePath = VfsUtil.getRelativePath(this, project.courseDir)!!
        course.getAdditionalFile(coursePath)
      }
    }

    for (path in paths) {
      val fullPath = if (pathPrefix.isEmpty()) path else "$pathPrefix/$path"
      val file = LightPlatformTestCase.getSourceRoot().findFileByRelativePath(fullPath) ?: error("Can't find `$path` file")
      selectedFiles += file

      if (file.isDirectory) {
        affectedCourseFiles += VfsUtil.collectChildrenRecursively(file).mapNotNull {
          if (it.isDirectory) return@mapNotNull null
          it.toEduFile()
        }
      } else {
        val visibleFile = file.toEduFile() ?: error("Can't convert `$path` to edu file")
        affectedCourseFiles += visibleFile
      }
    }

    val dataContext = if (selectedFiles.size == 1) {
      dataContext(selectedFiles.single())
    } else {
      dataContext(selectedFiles.toTypedArray())
    }

    val hide = getActionById<CCChangeFileVisibility>(CCHideFromLearner.ACTION_ID)
    val show = getActionById<CCChangeFileVisibility>(CCMakeVisibleToLearner.ACTION_ID)
    checkAction(hide, show, dataContext, affectedCourseFiles, shouldActionBeEnabled, shouldOppositeActionBeEnabled)
    affectedCourseFiles.forEach { it.isVisible = !it.isVisible}
    checkAction(show, hide, dataContext, affectedCourseFiles, shouldActionBeEnabled, shouldOppositeActionBeEnabled)
  }

  private fun createCourse(
    courseMode: CourseMode
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

    additionalFile("a.txt")
    additionalFile("a-visible.txt") {
      withVisibility(true)
    }

    additionalFile("dir/x.txt")
    additionalFile("dir/y.txt")
    additionalFile("dir/x-visible.txt") {
      withVisibility(true)
    }
    additionalFile("dir/y-visible.txt") {
      withVisibility(true)
    }
  }

  private fun checkAction(
    action: CCChangeFileVisibility,
    oppositeAction: CCChangeFileVisibility,
    dataContext: DataContext,
    affectedCourseFiles: List<EduFile>,
    shouldActionBeEnabled: Boolean = true,
    shouldOppositeActionBeEnabled: Boolean = false
  ) {
    val initialStates = affectedCourseFiles.associateWith { it.isVisible }
    val initialPlaceholders = affectedCourseFiles.associateWith { (it as? TaskFile)?.answerPlaceholders }

    testAction(oppositeAction, dataContext, shouldBeEnabled = shouldOppositeActionBeEnabled, runAction = false)
    testAction(action, dataContext, shouldBeEnabled = shouldActionBeEnabled)

    if (shouldActionBeEnabled) {
      for (visibleFile in affectedCourseFiles) {
        assertEquals(action.requiredVisibility, visibleFile.isVisible)
        if (!action.requiredVisibility && visibleFile is TaskFile) {
          check(visibleFile.answerPlaceholders.isEmpty()) {
            "Task file `${visibleFile.name}` shouldn't contain placeholders because it's invisible"
          }
        }
      }
      UndoManager.getInstance(project).undo(null)
      for (visibleFile in affectedCourseFiles) {
        assertEquals(initialStates[visibleFile], visibleFile.isVisible)
        if (visibleFile is TaskFile) {
          assertContentEquals(initialPlaceholders[visibleFile], visibleFile.answerPlaceholders)
        }
      }
    }
  }
}
