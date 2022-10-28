package com.jetbrains.edu.coursecreator.actions

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.coursecreator.actions.checkAllTasks.CCCheckAllTasksAction
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CheckAllTest : EduActionTestCase() {
  private lateinit var connection: MessageBusConnection

  override fun setUp() {
    super.setUp()
    connection = project.messageBus.connect(testRootDisposable)
  }

  fun `test all solved`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          checkResultFile(CheckStatus.Solved)
        }
      }
    }

    val dataContext = dataContext(arrayOf(getCourseDir()))

    doTestWithNotification(dataContext) {
      assertEquals(EduCoreBundle.message("notification.content.all.tasks.solved.correctly"), it.content)
    }
  }

  fun `test failed tasks`() {
    val taskName = "Failed Task"
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask(name = taskName) {
          checkResultFile(CheckStatus.Failed)
        }
      }
    }

    val dataContext = dataContext(arrayOf(getCourseDir()))

    doTestWithNotification(dataContext) {
      assertEquals("1 of 1 tasks failed", it.subtitle)
      assertEquals("<a href=\"0\">lesson1/$taskName</a>", it.content)
    }
  }

  fun `test tasks with different statuses`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          checkResultFile(CheckStatus.Unchecked)
        }
        eduTask {
          checkResultFile(CheckStatus.Solved)
        }
      }
      lesson {
        eduTask {
          checkResultFile(CheckStatus.Failed)
        }
      }
    }

    val dataContext = dataContext(arrayOf(getCourseDir()))

    doTestWithNotification(dataContext) {
      assertEquals("2 of 3 tasks failed", it.subtitle)
      assertEquals("<a href=\"0\">lesson1/task1</a><br><a href=\"1\">lesson2/task1</a>", it.content)
    }
  }

  fun `test disabled in student mode`() {
    courseWithFiles {
      lesson {
        eduTask {
          checkResultFile(CheckStatus.Solved)
        }
      }
    }
    testAction(CCCheckAllTasksAction.ACTION_ID, dataContext(emptyArray()), shouldBeEnabled = false)
  }

  fun `test check tasks with one data context`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson { createLessonContent() }   // 2 tasks
      section { createSectionContent() } // 4 tasks
      section { createSectionContent() } // 4 tasks
    }

    val dataContext = dataContext(arrayOf(getCourseDir()))

    doTestWithNotification(dataContext) {
      assertEquals("10 of 10 tasks failed", it.subtitle)
      val expectedStr = listOf(
        "<a href=\"0\">lesson1/task1</a>",
        "<a href=\"1\">lesson1/task2</a>",
        "<a href=\"2\">section2/lesson1/task1</a>",
        "<a href=\"3\">section2/lesson1/task2</a>",
        "<a href=\"4\">section2/lesson2/task1</a>",
        "<a href=\"5\">section2/lesson2/task2</a>",
        "<a href=\"6\">section3/lesson1/task1</a>",
        "<a href=\"7\">section3/lesson1/task2</a>",
        "<a href=\"8\">section3/lesson2/task1</a>",
        "<a href=\"9\">section3/lesson2/task2</a>",
        ).joinToString("<br>")
      assertEquals(expectedStr, it.content)
    }
  }

  fun `test check tasks with multiple data context`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section { createSectionContent() }  // 4 tasks
      section { createSectionContent() } // 4 tasks
      section { createSectionContent() } // 4 tasks
    }

    fun courseCreationError(): Nothing = error("course was not created correctly")

    val courseDir = getCourseDir()
    val task = courseDir.findChild("section1")?.findChild("lesson1")?.findChild("task1") ?: courseCreationError()
    val lesson = courseDir.findChild("section2")?.findChild("lesson2") ?: courseCreationError()
    val section = courseDir.findChild("section3") ?: courseCreationError()

    val skippedLesson = courseDir.findChild("section3")?.findChild("lesson1") ?: courseCreationError()
    val skippedTask = courseDir.findChild("section2")?.findChild("lesson2")?.findChild("task1") ?: courseCreationError()

    val dataContext = dataContext(arrayOf(
      task,
      lesson,
      section,
      skippedLesson,
      skippedTask,
    ))

    doTestWithNotification(dataContext) {
      assertEquals("7 of 7 tasks failed", it.subtitle)
      val expectedStr = listOf(
        "<a href=\"0\">section3/lesson1/task1</a>",
        "<a href=\"1\">section3/lesson1/task2</a>",
        "<a href=\"2\">section3/lesson2/task1</a>",
        "<a href=\"3\">section3/lesson2/task2</a>",
        "<a href=\"4\">section2/lesson2/task1</a>",
        "<a href=\"5\">section2/lesson2/task2</a>",
        "<a href=\"6\">section1/lesson1/task1</a>",
      ).joinToString("<br>")
      assertEquals(expectedStr, it.content)
    }
  }

  private fun LessonBuilder<Lesson>.createLessonContent() {
    eduTask {
      checkResultFile(CheckStatus.Failed)
    }
    eduTask {
      checkResultFile(CheckStatus.Failed)
    }
  }

  private fun SectionBuilder.createSectionContent() {
    lesson {
      createLessonContent()
    }
    lesson {
      createLessonContent()
    }
  }

  private fun doTestWithNotification(dataContext: DataContext = dataContext(emptyArray()), checkNotification: (Notification) -> Unit) {
    var notificationShown = false
    connection.subscribe(Notifications.TOPIC, object: Notifications {
      override fun notify(notification: Notification) {
        notificationShown = true
        checkNotification(notification)
      }
    })

    testAction(CCCheckAllTasksAction.ACTION_ID, dataContext)
    assertTrue("Notification wasn't shown", notificationShown)
  }

  private fun getCourseDir(): VirtualFile = LightPlatformTestCase.getSourceRoot()
}