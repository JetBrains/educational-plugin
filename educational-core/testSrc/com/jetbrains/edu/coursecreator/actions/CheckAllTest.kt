package com.jetbrains.edu.coursecreator.actions

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.notification.NotificationsAdapter
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.CheckStatus

class CheckAllTest : EduActionTestCase() {
  private lateinit var connection: MessageBusConnection

  override fun setUp() {
    super.setUp()
    connection = project.messageBus.connect(testRootDisposable)
  }

  fun `test all solved`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          checkResultFile(CheckStatus.Solved)
        }
      }
    }

    doTestWithNotification {
      assertEquals(CheckAllTasks.SUCCESS_MESSAGE , it.content)
    }
  }

  fun `test failed tasks`() {
    val taskName = "Failed Task"
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask(name = taskName) {
          checkResultFile(CheckStatus.Failed)
        }
      }
    }

    doTestWithNotification {
      assertEquals("1 of 1 tasks failed", it.subtitle)
      assertEquals("<a href=\"0\">lesson1/$taskName</a>", it.content)
    }
  }

  fun `test tasks with different statuses`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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

    doTestWithNotification {
      assertEquals("2 of 3 tasks failed", it.subtitle)
      assertEquals("<a href=\"0\">lesson1/task1</a><br><a href=\"1\">lesson2/task1</a>", it.content)
    }
  }

  private fun doTestWithNotification(checkNotification: (Notification) -> Unit) {
    var notificationShown = false
    connection.subscribe(Notifications.TOPIC, object: NotificationsAdapter() {
      override fun notify(notification: Notification) {
        notificationShown = true
        checkNotification(notification)
      }
    })

    testAction(dataContext(emptyArray()), CheckAllTasks())
    assertTrue("Notification wasn't shown", notificationShown)
  }
}