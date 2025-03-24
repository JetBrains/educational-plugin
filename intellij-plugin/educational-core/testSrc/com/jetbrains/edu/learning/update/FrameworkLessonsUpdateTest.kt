package com.jetbrains.edu.learning.update

import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.learning.FileTreeBuilder
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.assertContentsEqual
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.testAction
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit.MINUTES

abstract class FrameworkLessonsUpdateTest<T : Course> : UpdateTestBase<T>() {
  @Before
  fun prepare() {
    initiateLocalCourse()
  }

  // TODO check local files after RevertTaskAction.ACTION_ID. Should be done after EDU-6365. Also, when revert works with non-editable files (no ticket by now)

  @Test
  fun `update unmodified next task`() {
    updateCourse {
      task2.mainFile.textContents = "fun taskMod2() {}"

      task2.nonEditFile.textContents = "val p = 141"
      task2.nonEditFile.isEditable = true

      task2.testsFile.textContents = "fun test2Mod() {}"
    }
    next()
    assertTaskFolder {
      file("Task.kt", "fun task() {}") // this propagatable file is propagated from the task1
      file("NonEdit.kt", "val p = 141")
      file("Tests2.kt", "fun test2Mod() {}")
    }
    assertContentsEqual(localCourse.task2, "Task.kt", "fun taskMod2() {}")
  }

  @Test
  fun `update modified next task`() {
    // modify next task
    next()
    typeText()
    previous()

    updateCourse {
      task2.mainFile.textContents = "fun taskMod2() {}"

      task2.nonEditFile.textContents = "val p = 141"
      task2.nonEditFile.isEditable = true
    }

    next()
    assertTaskFolder {
      file("Task.kt", "/*comment*/fun task() {}") // this propagatable file is already changed by user, so it is not updated
      file("NonEdit.kt", "val p = 141")
      file("Tests2.kt", "fun test2() {}")
    }
  }

  @Test
  fun `update unmodified previous task`() {
    next()

    updateCourse {
      task1.mainFile.textContents = "fun taskMod1() {}"
      task1.nonEditFile.textContents = "val p = 141"
    }

    assertTaskFolder {
      file("Task.kt", "fun taskMod1() {}") // we see updated code because the task is not modified, so code must be propagated
      file("NonEdit.kt", "val p = 42")
      file("Tests2.kt", "fun test2() {}")
    }
    previous()
    assertTaskFolder {
      file("Task.kt", "fun taskMod1() {}") // we see updated code because the task was not modified
      file("NonEdit.kt", "val p = 141") // new version of the file
      file("Tests1.kt", "fun test1() {}")
    }
  }

  @Test
  fun `update modified previous task`() {
    typeText()

    next()

    updateCourse {
      task1.mainFile.textContents = "fun taskMod1() {}"
      task1.nonEditFile.textContents = "val p = 141"
    }

    assertTaskFolder {
      file("Task.kt", "/*comment*/fun task() {}")
      file("NonEdit.kt", "val p = 42")
      file("Tests2.kt", "fun test2() {}")
    }
    previous()
    assertTaskFolder {
      file("Task.kt", "/*comment*/fun task() {}") // we see user's modifications and not the updated code
      file("NonEdit.kt", "val p = 141") // new version of the non-propagatable file
      file("Tests1.kt", "fun test1() {}")
    }
  }

  @Test
  fun `update unmodified current task if it is first`() {
    updateCourse {
      task1.mainFile.textContents = "fun taskMod1() {}"

      task1.nonEditFile.textContents = "val p = 141"
      // make the file propagatable
      task1.nonEditFile.isEditable = true
      task2.nonEditFile.isEditable = true
      task3.nonEditFile.isEditable = true
    }

    assertTaskFolder {
      file("Task.kt", "fun taskMod1() {}")
      file("NonEdit.kt", "val p = 141") // the file became propagatable after the update, but the task is not modified, so we see the new text
      file("Tests1.kt", "fun test1() {}")
    }
  }

  @Test
  fun `update unmodified current task if it is second`() {
    next()
    updateCourse {
      task2.mainFile.textContents = "fun taskMod1() {}"

      task2.nonEditFile.textContents = "val p = 141"
      // make the file propagatable
      task2.nonEditFile.isEditable = true
      task3.nonEditFile.isEditable = true
    }

    assertTaskFolder {
      file("Task.kt", "fun task() {}") // we propagate from the previous task
      file("NonEdit.kt", "val p = 141") // the file became propagatable after the update, so we take its first propagatable text
      file("Tests2.kt", "fun test2() {}")
    }
  }

  @Test
  fun `update modified current task`() {
    typeText()
    updateCourse {
      task1.mainFile.textContents = "fun taskMod1() {}"

      task1.nonEditFile.textContents = "val p = 141"
      // make the file propagatable
      task1.nonEditFile.isEditable = true
      task2.nonEditFile.isEditable = true
      task3.nonEditFile.isEditable = true
    }

    assertTaskFolder {
      file("Task.kt", "/*comment*/fun task() {}")
      file("NonEdit.kt", "val p = 141") // the file became propagatable after the update, but it was not modified, so we see its new contents
      file("Tests1.kt", "fun test1() {}")
    }
  }

  @Test
  fun `update description`() {
    updateCourse {
      task1.descriptionText = "New Description"
      task2.descriptionFormat = DescriptionFormat.MD
    }

    with(localCourse) {
      assertEquals(Date(MINUTES.toMillis(2)), task1.updateDate)
      assertEquals(Date(MINUTES.toMillis(2)), task2.updateDate)
      assertEquals("New Description", task1.descriptionText)
      assertEquals(DescriptionFormat.MD, task2.descriptionFormat)
    }
  }

  @Test
  fun `add new task file`() {
    updateCourse {
      task1.addTaskFile(TaskFile("NewFileProp.kt", "boo 1"))
      task1.addTaskFile(TaskFile("NewFileNonProp.kt", "boo 1", isVisible = false))

      task2.addTaskFile(TaskFile("NewFileProp.kt", "boo 2"))
      task2.addTaskFile(TaskFile("NewFileNonProp.kt", "boo 2", isVisible = false))

      task3.addTaskFile(TaskFile("NewFileProp.kt", "boo 3"))
      task3.addTaskFile(TaskFile("NewFileNonProp.kt", "boo 3", isVisible = false))
    }

    assertTaskFolder {
      file("Task.kt", "fun task() {}")
      file("NonEdit.kt", "val p = 41")
      file("Tests1.kt", "fun test1() {}")
      file("NewFileProp.kt", "boo 1")
      file("NewFileNonProp.kt", "boo 1")
    }

    next()

    assertTaskFolder {
      file("Task.kt", "fun task() {}")
      file("NonEdit.kt", "val p = 42")
      file("Tests2.kt", "fun test2() {}")
      file("NewFileProp.kt", "boo 1")
      file("NewFileNonProp.kt", "boo 2")
    }
  }

  @Test
  fun `remove task file`() {
    // add one more propagatable file to the lesson so that we can remove it
    with(localCourse) {
      task1.addTaskFile(TaskFile("Task2.kt", "boo 1"))
      task2.addTaskFile(TaskFile("Task2.kt", "boo 2"))
      task3.addTaskFile(TaskFile("Task2.kt", "boo 3"))
    }

    updateCourse {
      task1.removeTaskFile("NonEdit.kt")
      task1.removeTaskFile("Task2.kt")

      task2.removeTaskFile("NonEdit.kt")
      task2.removeTaskFile("Task2.kt")

      task3.removeTaskFile("NonEdit.kt")
      task3.removeTaskFile("Task2.kt")
    }

    assertTaskFolder {
      file("Task.kt", "fun task() {}")
      file("Tests1.kt", "fun test1() {}")
    }

    next()

    assertTaskFolder {
      file("Task.kt", "fun task() {}")
      file("Tests2.kt", "fun test2() {}")
    }
  }

  @Test
  fun `test check current task index and task record saved`() {
    next()

    val taskRecordIndexBeforeUpdate = localCourse.task1.record

    assertEquals(1, frameworkLesson.currentTaskIndex)
    assertNotSame(-1, taskRecordIndexBeforeUpdate)

    val taskText = "fun foo2() {}"
    val testText = """
      fun test1() {}
      fun test2() {}
    """.trimIndent()

    updateCourse {
      task2.mainFile.textContents = taskText
      task2.testsFile.textContents = testText
    }

    assertEquals(1, frameworkLesson.currentTaskIndex)
    assertEquals("Record index should be preserved after update", taskRecordIndexBeforeUpdate, localCourse.task1.record)
  }

  @Test
  fun `new tasks in the end of framework lessons update correctly`() {
    with (localCourse) {
      task1.status = CheckStatus.Solved
      task2.status = CheckStatus.Solved
      task3.status = CheckStatus.Solved
    }

    updateCourse {
      val newTask = EduTask("task4").apply {
        id = 4
        description = "Old Description"
        descriptionFormat = DescriptionFormat.HTML
      }
      newTask.addTaskFile(TaskFile("Task.kt", "fun task() {}"))
      newTask.addTaskFile(TaskFile("NonEdit.kt", "val p = 44").apply {
        isEditable = false
      })
      lessons[0].addTask(newTask)

      if (this is HyperskillCourse) {
        stages += HyperskillStage(4, "", 4)
      }
    }

    assertEquals(4, localCourse.taskList.size)

    next()
    next()
    next()
    assertTaskFolderWithoutCourseFolder {
      file("Task.kt", "fun task() {}")
      file("NonEdit.kt", "val p = 44")
    }
  }

  protected abstract fun produceCourse(): T

  protected abstract fun setupLocalCourse(course: T)

  override fun initiateLocalCourse() {
    @Suppress("UNCHECKED_CAST")
    val eduCourse = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = { produceCourse() }
    ) {
      frameworkLesson("lesson1", isTemplateBased = false) {
        eduTask("task1", stepId = 1, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("Task.kt", "fun task() {}", visible = true, editable = true)
          taskFile("NonEdit.kt", "val p = 41", visible = true, editable = false)
          taskFile("Tests1.kt", "fun test1() {}", visible = false, editable = false)
        }
        eduTask("task2", stepId = 2, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("Task.kt", "fun task() {}", visible = true, editable = true)
          taskFile("NonEdit.kt", "val p = 42", visible = true, editable = false)
          taskFile("Tests2.kt", "fun test2() {}", visible = false, editable = false)
        }
        eduTask("task3", stepId = 3, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("Task.kt", "fun task() {}", visible = true, editable = true)
          taskFile("NonEdit.kt", "val p = 43", visible = true, editable = false)
          taskFile("Tests3.kt", "fun test3() {}", visible = false, editable = false)
        }
      }
    } as T
    setupLocalCourse(eduCourse)
    localCourse = eduCourse
  }

  protected fun assertTaskFolder(treeBuilder: FileTreeBuilder.() -> Unit) = runInEdtAndWait {
    fileTree {
      dir("lesson1") {
        dir("task") {
          treeBuilder()
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  protected fun assertTaskFolderWithoutCourseFolder(treeBuilder: FileTreeBuilder.() -> Unit) = runInEdtAndWait {
    fileTree(treeBuilder).assertEquals(LightPlatformTestCase.getSourceRoot().findFileByRelativePath("lesson1/task")!!, myFixture)
  }

  protected fun openTaskInEditor() = runInEdtAndWait {
    withVirtualFileListener(localCourse) {
      currentTask.openTaskFileInEditor("Task.kt")
    }
  }

  protected fun typeText(path: String = "Task.kt", newText: String = "/*comment*/") = runInEdtAndWait {
    withVirtualFileListener(localCourse) {
      currentTask.openTaskFileInEditor(path)
      myFixture.type(newText)
    }
  }

  protected fun next() = runInEdtAndWait {
    openTaskInEditor()
    withVirtualFileListener(localCourse) {
      testAction(NextTaskAction.ACTION_ID)
    }
  }

  protected fun previous() = runInEdtAndWait {
    withVirtualFileListener(localCourse) {
      testAction(PreviousTaskAction.ACTION_ID)
    }
  }

  fun updateCourse(isShouldBeUpdated: Boolean = true, courseChanger: T.() -> Unit) = updateCourse(toRemoteCourse {
    courseChanger()
    val updateDate = Date(MINUTES.toMillis(2))
    lessons[0].taskList.forEach { it.updateDate = updateDate }
  }, isShouldBeUpdated = isShouldBeUpdated)

  protected val T.taskList: List<Task> get() = lessons[0].taskList
  protected val frameworkLesson: FrameworkLesson get() = localCourse.lessons[0] as FrameworkLesson
  protected val currentTask: Task get() = frameworkLesson.currentTask()!!

  protected val T.task1: Task get() = lessons[0].taskList[0]
  protected val T.task2: Task get() = lessons[0].taskList[1]
  protected val T.task3: Task get() = lessons[0].taskList[2]
  protected val Task.mainFile: TaskFile get() = taskFiles["Task.kt"]!!
  protected val Task.nonEditFile: TaskFile get() = taskFiles["NonEdit.kt"]!!
  protected val Task.testsFile: TaskFile get() = taskFiles["Tests1.kt"] ?: taskFiles["Tests2.kt"] ?: taskFiles["Tests3.kt"]!!
  protected var TaskFile.textContents: String
    get() = (contents as TextualContents).text
    set(value) {
      contents = InMemoryTextualContents(value)
    }
}