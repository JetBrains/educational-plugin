package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.init
import com.jetbrains.edu.learning.update.UpdatesAvailableTestBase
import kotlinx.coroutines.runBlocking

class HyperskillLessonUpdatesAvailableTest : UpdatesAvailableTestBase<HyperskillCourse>() {
  @Suppress("SameParameterValue")
  private fun doTestUpdatesAvailable(remoteCourse: HyperskillCourse, expectedAmountOfUpdates: Int) {
    initiateLocalCourse()
    createCourseStructure(localCourse)
    val updater = HyperskillLessonUpdater(project, localCourse)
    val updates = runBlocking {
      updater.collect(remoteCourse)
    }
    assertEquals(expectedAmountOfUpdates, updates.size)
  }

  fun `test updates are available when task name is changed`() {
    val serverCourse = course(courseProducer = ::HyperskillCourse) {
      lesson("lesson1") {
        eduTask("task1 updated", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  fun `test updates are available when new task created`() {
    val serverCourse = course(courseProducer = ::HyperskillCourse) {
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
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  fun `test updates are available when existing task has been deleted`() {
    val serverCourse = course(courseProducer = ::HyperskillCourse) {
      lesson("lesson1") {
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  fun `test updates are available when task places are changed`() {
    val serverCourse = course(courseProducer = ::HyperskillCourse) {
      lesson("lesson1") {
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
      }
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  fun `test updates are available when new lesson created`() {
    val serverCourse = course(courseProducer = ::HyperskillCourse) {
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
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  override fun initiateLocalCourse() {
    localCourse = course(courseProducer = ::HyperskillCourse) {
      lesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    } as HyperskillCourse
    localCourse.init(1, false)
  }
}