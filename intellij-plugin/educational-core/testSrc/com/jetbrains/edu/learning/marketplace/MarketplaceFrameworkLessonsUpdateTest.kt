package com.jetbrains.edu.learning.marketplace

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduExperimentalFeatures.NEW_COURSE_UPDATE
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.assertContentsEqual
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdater
import com.jetbrains.edu.learning.stepik.hyperskill.FrameworkLessonsUpdateTest
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.rules.WithExperimentalFeature
import org.junit.Test
import kotlin.test.assertNotEquals

@WithExperimentalFeature(id = NEW_COURSE_UPDATE, value = false)
class MarketplaceFrameworkLessonsUpdateTest : FrameworkLessonsUpdateTest<EduCourse>() {

  @Test
  fun `test check current task index and task record saved`() {
    createCourseWithFrameworkLessons()

    val task1 = localCourse.taskList[0]

    withVirtualFileListener(localCourse) {
      task1.openTaskFileInEditor("src/Task.kt")
      testAction(NextTaskAction.ACTION_ID)
    }

    val taskRecordIndexBeforeUpdate = task1.record

    assertEquals(1, getFirstLesson()?.currentTaskIndex)
    assertNotEquals(-1, taskRecordIndexBeforeUpdate)

    val taskText = "fun foo2() {}"
    val testText = """
      fun test1() {}
      fun test2() {}
    """.trimIndent()
    updateCourse {
      taskList[1].apply {
        taskFiles["src/Task.kt"]!!.text = taskText
        taskFiles["test/Tests2.kt"]!!.text = testText
      }
    }

    val task = localCourse.taskList[1]
    assertEquals(taskText, task.taskFiles["src/Task.kt"]!!.text)
    assertEquals(testText, task.taskFiles["test/Tests2.kt"]!!.text)
    assertEquals(1, getFirstLesson()?.currentTaskIndex)
    assertEquals("Record index should be preserved after update", taskRecordIndexBeforeUpdate, localCourse.taskList[0].record)

    fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", taskText)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests2.kt", testText)
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  @Test
  fun `test non-editable files addition and change update correctly`() {
    val taskNum = 3
    val eduCourse = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse
    ) {
      frameworkLesson("lesson1") {
        for (index in 1..taskNum) {
          eduTask("task$index", stepId = index, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Baz.kt", "fun baz() {}", editable = false)
          }
        }
      }
    } as EduCourse
    eduCourse.marketplaceCourseVersion = 1
    localCourse = eduCourse

    val task1 = localCourse.taskList[0]

    withVirtualFileListener(localCourse) {
      task1.openTaskFileInEditor("src/Baz.kt")
      testAction(NextTaskAction.ACTION_ID)
    }

    val bazText = "fun bazbaz() {}"
    val barText = "fun barbar() {}"
    updateCourse {
      for (task in taskList) {
        task.apply {
          taskFiles["src/Baz.kt"]!!.contents = InMemoryTextualContents(bazText)

          addTaskFile("src/Bar.kt").apply {
            text = barText
            isEditable = false
          }
        }
      }
    }

    for (index in 1..taskNum) {
      val bazTaskFile = localCourse.taskList[index - 1].taskFiles["src/Baz.kt"]!!
      val barTaskFile = localCourse.taskList[index - 1].taskFiles["src/Bar.kt"]!!

      assertEquals(bazText, bazTaskFile.contents.textualRepresentation)
      assertEquals(barText, barTaskFile.contents.textualRepresentation)

      assertFalse(bazTaskFile.isEditable)
      assertFalse(barTaskFile.isEditable)
    }

    fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Baz.kt", bazText)
            file("Bar.kt", barText)
          }
        }
        for (index in 1..taskNum) {
          dir("task$index") {
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test new tasks in the end of framework lessons update correctly`() {
    val taskNum = 2

    val bazText = "fun baz() {}"

    val eduCourse = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse
    ) {
      frameworkLesson("lesson1") {
        for (index in 1..taskNum) {
          eduTask("task$index", stepId = index, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Baz.kt", bazText)
          }
        }
      }
    } as EduCourse
    eduCourse.marketplaceCourseVersion = 1
    localCourse = eduCourse

    val newTasksNum = 2

    val newBazText = "fun newBaz() {}"
    val barText = "fun bar() {}"

    updateCourse {
      repeat(newTasksNum) { index ->
        val newTask = EduTask("newTask${index + 1}").apply {
          val taskFile1 = TaskFile("src/Baz.kt", InMemoryTextualContents(newBazText))
          taskFile1.addAnswerPlaceholder(AnswerPlaceholder(1, "TODO(1)"))
          addTaskFile(taskFile1)

          val taskFile2 = TaskFile("src/Bar.kt", InMemoryTextualContents(barText))
          taskFile2.addAnswerPlaceholder(AnswerPlaceholder(2, "TODO(2)"))
          addTaskFile(taskFile2)
          descriptionText = "New Description"
          descriptionFormat = DescriptionFormat.HTML
        }
        lessons[0].addTask(newTask)
      }
    }

    assertTrue(localCourse.taskList.size == taskNum + newTasksNum)

    for (index in 1..taskNum) {
      assertContentsEqual(localCourse.taskList[index - 1], "src/Baz.kt", bazText)
    }

    for (index in 1..newTasksNum) {
      val task = localCourse.taskList[taskNum + index - 1]
      assertContentsEqual(task, "src/Baz.kt", newBazText)
      assertContentsEqual(task, "src/Bar.kt", barText)
      val bazAnswerPlaceholders = task.taskFiles["src/Baz.kt"]!!.answerPlaceholders
      val barAnswerPlaceholders = task.taskFiles["src/Bar.kt"]!!.answerPlaceholders
      assertEquals("lesson1#${task.name}#src/Baz.kt[1, 8]", bazAnswerPlaceholders.single().toString())
      assertEquals("lesson1#${task.name}#src/Bar.kt[2, 9]", barAnswerPlaceholders.single().toString())
      assertEquals("New Description", task.descriptionText)
      assertEquals(DescriptionFormat.HTML, task.descriptionFormat)
    }

    val task1 = localCourse.taskList[0]

    withVirtualFileListener(localCourse) {
      task1.openTaskFileInEditor("src/Baz.kt")
      testAction(NextTaskAction.ACTION_ID)
      testAction(NextTaskAction.ACTION_ID)
      testAction(NextTaskAction.ACTION_ID)
    }

    fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Baz.kt", newBazText)
            file("Bar.kt", barText)
          }
        }
        for (index in 1..taskNum) {
          dir("task$index") {
            file("task.html")
          }
        }
        for (index in 1..newTasksNum) {
          dir("newTask$index") {
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir, myFixture)
  }

  override fun updateCourse(changeCourse: EduCourse.() -> Unit) {
    val remoteCourse = toRemoteCourse(changeCourse)
    MarketplaceCourseUpdater(project, localCourse, remoteCourse.marketplaceCourseVersion).updateCourseWithRemote(remoteCourse)
    assertEquals(remoteCourse.marketplaceCourseVersion, localCourse.marketplaceCourseVersion)
  }

  override fun createCourseWithFrameworkLessons() {
    val eduCourse = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun test1() {}")

        }
        eduTask("task2", stepId = 2, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests2.kt", "fun test2() {}")
        }
      }
    } as EduCourse
    eduCourse.marketplaceCourseVersion = 1
    localCourse = eduCourse
  }

  override fun toRemoteCourse(changeCourse: EduCourse.() -> Unit): EduCourse {
    val remoteCourse = localCourse.copy()
    copyFileContents(localCourse, remoteCourse)
    remoteCourse.init(false)
    remoteCourse.changeCourse()
    return remoteCourse
  }

  private fun getFirstLesson(): FrameworkLesson? = (localCourse.lessons[0] as? FrameworkLesson)
}