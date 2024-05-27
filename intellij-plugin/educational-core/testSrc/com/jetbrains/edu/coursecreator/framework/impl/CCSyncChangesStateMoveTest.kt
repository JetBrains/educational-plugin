package com.jetbrains.edu.coursecreator.framework.impl

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.framework.SyncChangesStateManager
import com.jetbrains.edu.coursecreator.framework.SyncChangesTaskFileState
import com.jetbrains.edu.learning.actions.move.MoveTestBase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import org.junit.Test

class CCSyncChangesStateMoveTest : MoveTestBase() {
  @Test
  fun `test state change after moving a file`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first()

    for (task in lesson.taskList) {
      for (taskFile in task.taskFiles.values) {
        assertNull(stateManager.getSyncChangesState(taskFile))
      }
    }

    val task1 = lesson.taskList[0]
    val task2 = lesson.taskList[1]

    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task1/src1/Bar.kt", "fun bar() {}")
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task2/src1/Bar.kt", "fun bar() {}")
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task3/src2/Bar.kt", "fun bar() {}")
    }
    task1.assertTaskFileState("src1/Bar.kt", SyncChangesTaskFileState.INFO)
    task2.assertTaskFileState("src1/Bar.kt", SyncChangesTaskFileState.WARNING)

    val sourceFile = findPsiFile("lesson1/task2/src1/Bar.kt")
    val targetDir = findPsiDirectory("lesson1/task2/src2")

    doMoveAction(course, sourceFile, targetDir)

    assertDoesntContain(task2.taskFiles.keys, "src1/Bar.kt")
    task1.assertTaskFileState("src1/Bar.kt", SyncChangesTaskFileState.WARNING)
    task2.assertTaskFileState("src2/Bar.kt", SyncChangesTaskFileState.INFO)
  }

  @Test
  fun `test state change after moving a folder`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first()

    for (task in lesson.taskList) {
      for (taskFile in task.taskFiles.values) {
        assertNull(stateManager.getSyncChangesState(taskFile))
      }
    }

    val task1 = lesson.taskList[0]
    val task2 = lesson.taskList[1]

    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task1/src2/Bar.kt", "fun bar() {}")
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task2/src2/Bar.kt", "fun bar() {}")
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task3/src1/src2/Bar.kt", "fun bar() {}")
    }

    task1.assertTaskFileState("src2/Bar.kt", SyncChangesTaskFileState.INFO)
    task2.assertTaskFileState("src2/Bar.kt", SyncChangesTaskFileState.WARNING)

    val sourceDir = findPsiDirectory("lesson1/task2/src2")
    val targetDir = findPsiDirectory("lesson1/task2/src1")

    doMoveAction(course, sourceDir, targetDir)

    assertDoesntContain(task2.taskFiles.keys, "src2/Bar.kt")
    task1.assertTaskFileState("src2/Bar.kt", SyncChangesTaskFileState.WARNING)
    task2.assertTaskFileState("src1/src2/Bar.kt", SyncChangesTaskFileState.INFO)
  }

  private fun createFrameworkCourse(): Course = courseWithFiles(
    courseMode = CourseMode.EDUCATOR,
    language = FakeGradleBasedLanguage
  ) {
    frameworkLesson("lesson1") {
      repeat(3) {
        eduTask("task${it + 1}") {
          taskFile("src1/Task.kt", "fun foo() {}")
          taskFile("src2/Baz.kt", "fun baz() {}")
          taskFile("test/Tests.kt", "fun tests() {}")
        }
      }
    }
  }

  private fun Task.assertTaskFileState(taskFileName: String, state: SyncChangesTaskFileState?) {
    val taskFile = getTaskFile(taskFileName)!!
    assertEquals(state, stateManager.getSyncChangesState(taskFile))
  }

  private val stateManager: SyncChangesStateManager
    get() = SyncChangesStateManager.getInstance(project)

  private val rootDir: VirtualFile
    get() = LightPlatformTestCase.getSourceRoot()
}