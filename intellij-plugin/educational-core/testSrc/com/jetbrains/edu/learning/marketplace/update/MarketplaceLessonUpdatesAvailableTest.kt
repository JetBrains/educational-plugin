package com.jetbrains.edu.learning.marketplace.update

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.update.UpdatesAvailableTestBase
import kotlinx.coroutines.runBlocking

class MarketplaceLessonUpdatesAvailableTest : UpdatesAvailableTestBase<EduCourse>() {
  @Suppress("SameParameterValue")
  private fun doTestUpdatesAvailable(remoteCourse: EduCourse, expectedAmountOfUpdates: Int) {
    initiateLocalCourse()
    createCourseStructure(localCourse)
    val updater = MarketplaceLessonUpdater(project, localCourse)
    val updates = runBlocking {
      updater.collect(remoteCourse)
    }
    assertEquals(expectedAmountOfUpdates, updates.size)
  }

  fun `test updates are available when task name is changed`() {
    val serverCourse = course {
      lesson("lesson1") {
        eduTask("task1 updated", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    } as EduCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  fun `test updates are available when new task created`() {
    val serverCourse = course {
      lesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
        eduTask("task3", stepId = 3) {
          taskFile("TaskFile3.kt", "task file 3 text")
        }
      }
    } as EduCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  fun `test updates are available when existing task has been deleted`() {
    val serverCourse = course {
      lesson("lesson1") {
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    } as EduCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  fun `test updates are available when task places are changed`() {
    val serverCourse = course {
      lesson("lesson1") {
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
      }
    } as EduCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  fun `test updates are available when new lesson created`() {
    val serverCourse = course {
      lesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
      lesson("lesson2") {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    } as EduCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  override fun initiateLocalCourse() {
    localCourse = course {
      lesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    }.apply { marketplaceCourseVersion = 1 } as EduCourse
  }
}