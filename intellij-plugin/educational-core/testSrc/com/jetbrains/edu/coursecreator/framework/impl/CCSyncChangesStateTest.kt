package com.jetbrains.edu.coursecreator.framework.impl

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vcs.merge.MergeSession
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.actions.CCSyncChangesWithNextTasks
import com.jetbrains.edu.coursecreator.framework.SyncChangesStateManager
import com.jetbrains.edu.coursecreator.framework.SyncChangesTaskFileState
import com.jetbrains.edu.coursecreator.framework.diff.withFLMultipleFileMergeUI
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class CCSyncChangesStateTest : EduActionTestCase() {
  @Test
  fun `test state change after saving document`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first() as FrameworkLesson

    val task = lesson.taskList.first()

    assertNull(stateManager.getSyncChangesState(task))
    assertNull(stateManager.getSyncChangesState(lesson))
    for (taskFile in task.taskFiles.values) {
      assertNull(stateManager.getSyncChangesState(taskFile))
    }

    withVirtualFileListener(course) {
      typeIntoFile(task, "src/Baz.kt", "fun baz() {}\n")
    }

    stateManager.waitForAllRequestsProcessed()

    assertEquals(SyncChangesTaskFileState.INFO, stateManager.getSyncChangesState(task))
    assertEquals(SyncChangesTaskFileState.INFO, stateManager.getSyncChangesState(lesson))
    task.assertTaskFileState("src/Baz.kt", SyncChangesTaskFileState.INFO)
  }

  @Test
  fun `test state change after adding a new file`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first() as FrameworkLesson
    val task = lesson.taskList.first()

    assertNull(stateManager.getSyncChangesState(task))
    assertNull(stateManager.getSyncChangesState(lesson))
    for (taskFile in task.taskFiles.values) {
      assertNull(stateManager.getSyncChangesState(taskFile))
    }

    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task1/src/Bar.kt", "fun bar() {}")
    }

    stateManager.waitForAllRequestsProcessed()

    assertEquals(SyncChangesTaskFileState.WARNING, stateManager.getSyncChangesState(task))
    assertEquals(SyncChangesTaskFileState.WARNING, stateManager.getSyncChangesState(lesson))
    task.assertTaskFileState("src/Bar.kt", SyncChangesTaskFileState.WARNING)
  }

  @Test
  fun `test state change in previous task after adding a new file`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first() as FrameworkLesson

    val task = lesson.taskList.first()

    assertNull(stateManager.getSyncChangesState(task))
    assertNull(stateManager.getSyncChangesState(lesson))
    for (taskFile in task.taskFiles.values) {
      assertNull(stateManager.getSyncChangesState(taskFile))
    }

    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task1/src/Bar.kt", "fun bar() {}")
      task.assertTaskFileState("src/Bar.kt", SyncChangesTaskFileState.WARNING)
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task2/src/Bar.kt", "fun bar() {}")
    }

    stateManager.waitForAllRequestsProcessed()

    assertEquals(SyncChangesTaskFileState.INFO, stateManager.getSyncChangesState(task))
    // because we have 3 tasks and we don't have this file in the third
    assertEquals(SyncChangesTaskFileState.WARNING, stateManager.getSyncChangesState(lesson))
    task.assertTaskFileState("src/Bar.kt", SyncChangesTaskFileState.INFO)
  }

  @Test
  fun `test state change in previous task after removing a file`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first() as FrameworkLesson

    val task = lesson.taskList.first()

    assertNull(stateManager.getSyncChangesState(task))
    assertNull(stateManager.getSyncChangesState(lesson))
    for (taskFile in task.taskFiles.values) {
      assertNull(stateManager.getSyncChangesState(taskFile))
    }

    withVirtualFileListener(course) {
      runWriteAction {
        findFile("lesson1/task2/src/Task.kt").delete(CCSyncChangesStateTest::class.java)
      }
    }

    stateManager.waitForAllRequestsProcessed()

    assertEquals(SyncChangesTaskFileState.WARNING, stateManager.getSyncChangesState(task))
    assertEquals(SyncChangesTaskFileState.WARNING, stateManager.getSyncChangesState(lesson))
    task.assertTaskFileState("src/Task.kt", SyncChangesTaskFileState.WARNING)
  }

  @Test
  fun `test states change in previous task after removing a dir`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first() as FrameworkLesson

    val task = lesson.taskList.first()

    assertNull(stateManager.getSyncChangesState(task))
    assertNull(stateManager.getSyncChangesState(lesson))
    for (taskFile in task.taskFiles.values) {
      assertNull(stateManager.getSyncChangesState(taskFile))
    }

    withVirtualFileListener(course) {
      runWriteAction {
        findFile("lesson1/task2/src").delete(CCSyncChangesStateTest::class.java)
      }
    }

    stateManager.waitForAllRequestsProcessed()

    assertEquals(SyncChangesTaskFileState.WARNING, stateManager.getSyncChangesState(task))
    assertEquals(SyncChangesTaskFileState.WARNING, stateManager.getSyncChangesState(lesson))
    task.assertTaskFileState("src/Task.kt", SyncChangesTaskFileState.WARNING)
    task.assertTaskFileState("src/Baz.kt", SyncChangesTaskFileState.WARNING)
  }

  @Test
  fun `test states change in previous task after removing a task`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first() as FrameworkLesson

    val task1 = lesson.taskList.first()
    val task2 = lesson.taskList[1]

    assertNull(stateManager.getSyncChangesState(task1))
    assertNull(stateManager.getSyncChangesState(task2))
    assertNull(stateManager.getSyncChangesState(lesson))
    for (taskFile in task1.taskFiles.values) {
      assertNull(stateManager.getSyncChangesState(taskFile))
    }

    for (taskFile in task2.taskFiles.values) {
      assertNull(stateManager.getSyncChangesState(taskFile))
    }

    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task1/src/Bar.kt", "fun bar() {}")
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task3/src/Bar.kt", "fun bar() {}")
      task1.assertTaskFileState("src/Bar.kt", SyncChangesTaskFileState.WARNING)
      runWriteAction {
        findFile("lesson1/task3/src/Task.kt").delete(CCSyncChangesStateTest::class.java)
      }

      task2.assertTaskFileState("src/Task.kt", SyncChangesTaskFileState.WARNING)
      runWriteAction {
        findFile("lesson1/task2").delete(CCSyncChangesStateTest::class.java)
      }
    }

    stateManager.waitForAllRequestsProcessed()

    assertEquals(SyncChangesTaskFileState.WARNING, stateManager.getSyncChangesState(task1))
    assertEquals(SyncChangesTaskFileState.WARNING, stateManager.getSyncChangesState(lesson))
    task1.assertTaskFileState("src/Bar.kt", SyncChangesTaskFileState.INFO)
    task1.assertTaskFileState("src/Task.kt", SyncChangesTaskFileState.WARNING)
  }

  @Test
  fun `test states clears after sync changes operation`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first() as FrameworkLesson

    val task = lesson.taskList.first()

    assertNull(stateManager.getSyncChangesState(task))
    assertNull(stateManager.getSyncChangesState(lesson))
    for (taskFile in task.taskFiles.values) {
      assertNull(stateManager.getSyncChangesState(taskFile))
    }

    withVirtualFileListener(course) {
      typeIntoFile(task, "src/Task.kt", "fun bar() {}\n")
      task.assertTaskFileState("src/Task.kt", SyncChangesTaskFileState.INFO)
      invokeSyncChangesAction(task, listOf(MergeSession.Resolution.AcceptedYours, MergeSession.Resolution.AcceptedYours))
    }

    stateManager.waitForAllRequestsProcessed()

    assertEquals(null, stateManager.getSyncChangesState(task))
    assertEquals(null, stateManager.getSyncChangesState(lesson))
    task.assertTaskFileState("src/Task.kt", null)
  }

  @Test
  fun `test states are always none for last task`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first() as FrameworkLesson

    val lastTask = lesson.taskList.last()

    assertNull(stateManager.getSyncChangesState(lastTask))
    assertNull(stateManager.getSyncChangesState(lesson))
    for (taskFile in lastTask.taskFiles.values) {
      assertNull(stateManager.getSyncChangesState(taskFile))
    }

    withVirtualFileListener(course) {
      typeIntoFile(lastTask, "src/Task.kt", "fun bar() {}\n")
    }

    stateManager.waitForAllRequestsProcessed()
    assertEquals(null, stateManager.getSyncChangesState(lastTask))
    assertEquals(null, stateManager.getSyncChangesState(lesson))
    for (taskFile in lastTask.taskFiles.values) {
      assertNull(stateManager.getSyncChangesState(taskFile))
    }
  }

  private fun createFrameworkCourse(): Course = courseWithFiles(
    courseMode = CourseMode.EDUCATOR,
    language = FakeGradleBasedLanguage
  ) {
    frameworkLesson("lesson1") {
      repeat(3) {
        eduTask("task${it + 1}") {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests.kt", "fun tests() {}")
        }
      }
    }
  }

  private fun invokeSyncChangesAction(
    item: StudyItem,
    resolutions: List<MergeSession.Resolution>,
    // cancel on conflict resolution with number [cancelOnConflict]
    cancelOnConflict: Int = Int.MAX_VALUE,
  ) {
    val mockUI = MockFLMultipleFileMergeUI(resolutions, cancelOnConflict)
    withFLMultipleFileMergeUI(mockUI) {
      val dataContext = dataContext(item.getDir(project.courseDir)!!)
      testAction(CCSyncChangesWithNextTasks.ACTION_ID, dataContext)
    }
  }

  private fun Task.assertTaskFileState(taskFileName: String, state: SyncChangesTaskFileState?) {
    val taskFile = getTaskFile(taskFileName)!!
    stateManager.waitForAllRequestsProcessed()
    assertEquals(state, stateManager.getSyncChangesState(taskFile))
  }

  private fun typeIntoFile(task: Task, filePath: String, text: String) {
    task.openTaskFileInEditor(filePath)
    myFixture.type(text)
    FileDocumentManager.getInstance().saveAllDocuments()
  }

  private val stateManager: SyncChangesStateManager
    get() = SyncChangesStateManager.getInstance(project)

  private val rootDir: VirtualFile
    get() = LightPlatformTestCase.getSourceRoot()
}