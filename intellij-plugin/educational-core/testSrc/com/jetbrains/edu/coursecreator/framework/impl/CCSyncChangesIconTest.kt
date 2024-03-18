package com.jetbrains.edu.coursecreator.framework.impl

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vcs.merge.MergeSession
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.actions.CCSyncChangesWithNextTasks
import com.jetbrains.edu.coursecreator.framework.diff.withFLMultipleFileMergeUI
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.SyncChangesTaskFileState
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.testAction

class CCSyncChangesIconTest : EduActionTestCase() {
  fun `test icon change after saving document`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first()

    val task = lesson.taskList.first()

    for (taskFile in task.taskFiles.values) {
      assertTrue(taskFile.syncChangesIcon == SyncChangesTaskFileState.NONE)
    }

    withVirtualFileListener(course) {
      task.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      FileDocumentManager.getInstance().saveAllDocuments()
    }

    assertTrue(task.getTaskFile("src/Task.kt")?.syncChangesIcon == SyncChangesTaskFileState.INFO)
  }

  fun `test icon change after adding a new file`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first()

    val task = lesson.taskList.first()

    for (taskFile in task.taskFiles.values) {
      assertTrue(taskFile.syncChangesIcon == SyncChangesTaskFileState.NONE)
    }

    withVirtualFileListener(course) {
      task.openTaskFileInEditor("src/Task.kt")
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task1/src/Bar.kt", "fun bar() {}")
      FileDocumentManager.getInstance().saveAllDocuments()
    }

    assertTrue(task.getTaskFile("src/Bar.kt")?.syncChangesIcon == SyncChangesTaskFileState.WARNING)
  }

  fun `test icon change in previous task after adding a new file`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first()

    val task = lesson.taskList.first()

    for (taskFile in task.taskFiles.values) {
      assertTrue(taskFile.syncChangesIcon == SyncChangesTaskFileState.NONE)
    }

    withVirtualFileListener(course) {
      task.openTaskFileInEditor("src/Task.kt")
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task1/src/Bar.kt", "fun bar() {}")
      assertTrue(task.getTaskFile("src/Bar.kt")?.syncChangesIcon == SyncChangesTaskFileState.WARNING)
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task2/src/Bar.kt", "fun bar() {}")
    }

    assertTrue(task.getTaskFile("src/Bar.kt")?.syncChangesIcon == SyncChangesTaskFileState.INFO)
  }

  fun `test icon change in previous task after removing a file`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first()

    val task = lesson.taskList.first()

    for (taskFile in task.taskFiles.values) {
      assertTrue(taskFile.syncChangesIcon == SyncChangesTaskFileState.NONE)
    }

    withVirtualFileListener(course) {
      runWriteAction {
        findFile("lesson1/task2/src/Task.kt").delete(CCSyncChangesIconTest::class.java)
      }
    }

    assertTrue(task.getTaskFile("src/Task.kt")?.syncChangesIcon == SyncChangesTaskFileState.WARNING)
  }

  fun `test icons change in previous task after removing a dir`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first()

    val task = lesson.taskList.first()

    for (taskFile in task.taskFiles.values) {
      assertTrue(taskFile.syncChangesIcon == SyncChangesTaskFileState.NONE)
    }

    withVirtualFileListener(course) {
      runWriteAction {
        findFile("lesson1/task2/src").delete(CCSyncChangesIconTest::class.java)
      }
    }

    assertTrue(task.getTaskFile("src/Task.kt")?.syncChangesIcon == SyncChangesTaskFileState.WARNING)
    assertTrue(task.getTaskFile("src/Baz.kt")?.syncChangesIcon == SyncChangesTaskFileState.WARNING)
  }

  fun `test icons change in previous task after removing a task`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first()

    val task1 = lesson.taskList.first()
    val task2 = lesson.taskList[1]

    for (taskFile in task1.taskFiles.values) {
      assertTrue(taskFile.syncChangesIcon == SyncChangesTaskFileState.NONE)
    }

    for (taskFile in task2.taskFiles.values) {
      assertTrue(taskFile.syncChangesIcon == SyncChangesTaskFileState.NONE)
    }

    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task1/src/Bar.kt", "fun bar() {}")
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task3/src/Bar.kt", "fun bar() {}")
      assertTrue(task1.getTaskFile("src/Bar.kt")?.syncChangesIcon == SyncChangesTaskFileState.WARNING)
      runWriteAction {
        findFile("lesson1/task3/src/Task.kt").delete(CCSyncChangesIconTest::class.java)
      }
      assertTrue(task2.getTaskFile("src/Task.kt")?.syncChangesIcon == SyncChangesTaskFileState.WARNING)
      runWriteAction {
        findFile("lesson1/task2").delete(CCSyncChangesIconTest::class.java)
      }
    }

    assertTrue(task1.getTaskFile("src/Bar.kt")?.syncChangesIcon == SyncChangesTaskFileState.INFO)
    assertTrue(task1.getTaskFile("src/Task.kt")?.syncChangesIcon == SyncChangesTaskFileState.WARNING)
  }

  fun `test icons clears after sync changes operation`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first()

    val task = lesson.taskList.first()

    for (taskFile in task.taskFiles.values) {
      assertTrue(taskFile.syncChangesIcon == SyncChangesTaskFileState.NONE)
    }

    withVirtualFileListener(course) {
      task.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      FileDocumentManager.getInstance().saveAllDocuments()
      assertTrue(task.getTaskFile("src/Task.kt")?.syncChangesIcon == SyncChangesTaskFileState.INFO)
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task1/src/Bar.kt", "fun bar() {}")
      doTest(task, listOf(MergeSession.Resolution.AcceptedYours, MergeSession.Resolution.AcceptedYours), 1)
    }

    assertTrue(task.getTaskFile("src/Task.kt")?.syncChangesIcon == SyncChangesTaskFileState.NONE)
  }

  fun `test icons are always none for last task`() {
    val course = createFrameworkCourse()
    val lesson = course.lessons.first()

    val lastTask = lesson.taskList.last()

    for (taskFile in lastTask.taskFiles.values) {
      assertTrue(taskFile.syncChangesIcon == SyncChangesTaskFileState.NONE)
    }

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("src/Task.kt")
      myFixture.type("fun bar() {}\n")
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task1/src/Bar.kt", "fun bar() {}")
      doTest(task, listOf(MergeSession.Resolution.AcceptedYours, MergeSession.Resolution.AcceptedYours), 1)
    }

    for (taskFile in lastTask.taskFiles.values) {
      assertTrue(taskFile.syncChangesIcon == SyncChangesTaskFileState.NONE)
    }
  }

  private fun createFrameworkCourse(): Course = createFrameworkCourseWithFiles(
    mapOf(
      "src/Task.kt" to "fun foo() {}",
      "src/Baz.kt" to "fun baz() {}",
      "test/Tests.kt" to "fun tests() {}",
    )
  )

  private fun createFrameworkCourseWithFiles(files: Map<String, String>): Course = courseWithFiles(
    courseMode = CourseMode.EDUCATOR,
    language = FakeGradleBasedLanguage
  ) {
    frameworkLesson("lesson1") {
      repeat(3) {
        eduTask("task${it + 1}") {
          for ((name, content) in files) {
            taskFile(name, content)
          }
        }
      }
    }
  }.apply {
    val task = course.findTask("lesson1", "task1")
    doTest(task, List(2) { MergeSession.Resolution.AcceptedYours })
  }

  private fun doTest(
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

  private val rootDir: VirtualFile
    get() = LightPlatformTestCase.getSourceRoot()
}