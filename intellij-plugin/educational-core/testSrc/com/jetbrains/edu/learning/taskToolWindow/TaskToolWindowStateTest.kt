package com.jetbrains.edu.learning.taskToolWindow

import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.PsiAwareFileEditorManagerImpl
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.executeSomeCoroutineTasksAndDispatchAllInvocationEvents
import com.intellij.testFramework.replaceService
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowFactory.Companion.STUDY_TOOL_WINDOW
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.junit.Test

class TaskToolWindowStateTest : EduTestCase() {

  override fun setUp() {
    super.setUp()
    // It's important to use production implementation of `FileEditorManager` because they emit events differently
    // and this test checks how the corresponding listeners work
    setProductionFileEditorManager()
    registerTaskDescriptionToolWindow()
  }

  @Test
  fun `open task tool window on first task file opening`() {
    // given
    val course = createTestCourse()
    val toolWindow = getTaskToolWindow()

    // when
    openFileInEditor(findFile("lesson1/task1/taskFile1.txt"))

    // then
    assertEquals(TaskToolWindowView.getInstance(project).currentTask, course.findTask("lesson1", "task1"))
    assertTrue("Task tool window should be shown", toolWindow.isVisible)
  }

  @Test
  fun `do not reopen task tool window on selection another task file of the same task`() {
    // given
    val course = createTestCourse()
    val toolWindow = getTaskToolWindow()

    // when
    openFileInEditor(findFile("lesson1/task1/taskFile1.txt"))
    toolWindow.hide()
    openFileInEditor(findFile("lesson1/task1/taskFile2.txt"))

    // then
    assertEquals(TaskToolWindowView.getInstance(project).currentTask, course.findTask("lesson1", "task1"))
    assertFalse("Task tool window should not be shown", toolWindow.isVisible)
  }

  @Test
  fun `do not reopen task tool window on selection another task file of another task`() {
    // given
    val course = createTestCourse()
    val toolWindow = getTaskToolWindow()

    // when
    openFileInEditor(findFile("lesson1/task1/taskFile1.txt"))
    openFileInEditor(findFile("lesson1/task2/taskFile3.txt"))
    toolWindow.hide()
    openFileInEditor(findFile("lesson1/task1/taskFile1.txt"))

    // then
    assertEquals(TaskToolWindowView.getInstance(project).currentTask, course.findTask("lesson1", "task1"))
    assertFalse("Task tool window should not be shown", toolWindow.isVisible)
  }

  @Test
  fun `reopen task tool window on opening task file of non-current task`() {
    // given
    val course = createTestCourse()
    val toolWindow = getTaskToolWindow()

    // when
    openFileInEditor(findFile("lesson1/task1/taskFile1.txt"))
    toolWindow.hide()
    openFileInEditor(findFile("lesson1/task2/taskFile3.txt"))

    // then
    assertEquals(TaskToolWindowView.getInstance(project).currentTask, course.findTask("lesson1", "task2"))
    assertTrue("Task tool window should be shown", toolWindow.isVisible)
  }

  @Test
  fun `do not open task description on opening non-task file`() {
    // given
    createTestCourse()
    val toolWindow = getTaskToolWindow()

    // when
    openFileInEditor(findFile("additionalFile.txt"))

    // then
    assertEquals(TaskToolWindowView.getInstance(project).currentTask, null)
    assertFalse("Task tool window should not be shown", toolWindow.isVisible)
  }

  @Test
  fun `do not change current task on opening non-task file`() {
    // given
    val course = createTestCourse()
    val toolWindow = getTaskToolWindow()

    // when
    openFileInEditor(findFile("lesson1/task1/taskFile1.txt"))
    openFileInEditor(findFile("additionalFile.txt"))

    // then
    assertEquals(TaskToolWindowView.getInstance(project).currentTask, course.findTask("lesson1", "task1"))
    assertTrue("Task tool window should be shown", toolWindow.isVisible)
  }

  @Test
  fun `closing task file doesn't close tool window and changes current task`() {
    // given
    val course = createTestCourse()
    val toolWindow = getTaskToolWindow()

    // when
    openFileInEditor(findFile("lesson1/task1/taskFile1.txt"))
    openFileInEditor(findFile("lesson1/task2/taskFile3.txt"))
    closeFile(findFile("lesson1/task2/taskFile3.txt"))

    // then
    assertEquals(TaskToolWindowView.getInstance(project).currentTask, course.findTask("lesson1", "task1"))
    assertTrue("Task tool window should be shown", toolWindow.isVisible)
  }

  @Test
  fun `closing task file doesn't open tool window and changes current task`() {
    // given
    val course = createTestCourse()
    val toolWindow = getTaskToolWindow()

    // when
    openFileInEditor(findFile("lesson1/task1/taskFile1.txt"))
    openFileInEditor(findFile("lesson1/task2/taskFile3.txt"))
    toolWindow.hide()
    closeFile(findFile("lesson1/task2/taskFile3.txt"))

    // then
    assertEquals(TaskToolWindowView.getInstance(project).currentTask, course.findTask("lesson1", "task1"))
    assertFalse("Task tool window should not be shown", toolWindow.isVisible)
  }

  @Test
  fun `closing last task file doesn't close tool window and reset current task`() {
    // given
    createTestCourse()
    val toolWindow = getTaskToolWindow()

    // when
    openFileInEditor(findFile("lesson1/task1/taskFile1.txt"))
    closeFile(findFile("lesson1/task1/taskFile1.txt"))

    // then
    assertEquals(TaskToolWindowView.getInstance(project).currentTask, null)
    assertTrue("Task tool window should be shown", toolWindow.isVisible)
  }

  @Test
  fun `closing non-task file doesn't change state`() {
    // given
    val course = createTestCourse()
    val toolWindow = getTaskToolWindow()

    // when
    openFileInEditor(findFile("lesson1/task1/taskFile1.txt"))
    openFileInEditor(findFile("additionalFile.txt"))
    closeFile(findFile("additionalFile.txt"))

    // then
    assertEquals(TaskToolWindowView.getInstance(project).currentTask, course.findTask("lesson1", "task1"))
    assertTrue("Task tool window should be shown", toolWindow.isVisible)
  }

  /**
   * Temporarily replace test implementation of `FileEditorManager` with `PsiAwareFileEditorManagerImpl`
   * which is async and used in production
   */
  private fun setProductionFileEditorManager() {
    project.putUserData(ALLOW_IN_LIGHT_PROJECT_KEY, true)
    Disposer.register(testRootDisposable) { project.putUserData(ALLOW_IN_LIGHT_PROJECT_KEY, null) }

    project.replaceService(
      FileEditorManager::class.java,
      PsiAwareFileEditorManagerImpl(project, (project as ComponentManagerEx).getCoroutineScope().childScope(name)),
      testRootDisposable
    )
  }

  private fun createTestCourse(): Course = courseWithFiles {
    lesson("lesson1") {
      eduTask("task1") {
        taskFile("taskFile1.txt")
        taskFile("taskFile2.txt")
      }
      eduTask("task2") {
        taskFile("taskFile3.txt")
        taskFile("taskFile4.txt")
      }
    }
    additionalFile("additionalFile.txt")
  }

  private fun getTaskToolWindow(): ToolWindow = ToolWindowManager.getInstance(project).getToolWindow(STUDY_TOOL_WINDOW)
    ?: error("Failed to find a tool window with $STUDY_TOOL_WINDOW id")

  private fun openFileInEditor(file: VirtualFile) {
    myFixture.openFileInEditor(file)
    // Since we use async production implementation of `FileEditorManager` instead of sync test implementation,
    // we need to wait when all events are processed
    executeSomeCoroutineTasksAndDispatchAllInvocationEvents(project)
  }

  private fun closeFile(file: VirtualFile) {
    FileEditorManager.getInstance(project).closeFile(file)
    // Since we use async production implementation of `FileEditorManager` instead of sync test implementation,
    // we need to wait when all events are processed
    executeSomeCoroutineTasksAndDispatchAllInvocationEvents(project)
  }
}
