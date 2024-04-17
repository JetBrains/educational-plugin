package com.jetbrains.edu.learning.marketplace.update

import com.intellij.util.Time
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.update.UpdatesAvailableTestBase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.*

class MarketplaceLessonUpdatesAvailableTest : UpdatesAvailableTestBase<EduCourse>() {
  @Suppress("SameParameterValue")
  private fun doTestUpdatesAvailable(remoteCourse: EduCourse, expectedAmountOfUpdates: Int) {
    createCourseStructure(localCourse)
    val updater = MarketplaceLessonUpdater(project, localCourse)
    val updates = runBlocking {
      updater.collect(remoteCourse)
    }
    assertEquals(expectedAmountOfUpdates, updates.size)
  }

  @Test
  fun `test updates are available when lesson name is changed`() {
    initiateLocalCourse()
    val serverCourse = course {
      lesson("lesson1 updated") {
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

  @Test
  fun `test updates are available when lesson index is changed`() {
    localCourse = course {
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

    val serverCourse = course {
      lesson("lesson2") {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
      lesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    } as EduCourse

    doTestUpdatesAvailable(serverCourse, 2)
  }

  @Test
  fun `test updates are available when task name is changed`() {
    initiateLocalCourse()
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

  @Test
  fun `test updates are available when new task created`() {
    initiateLocalCourse()
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

  @Test
  fun `test updates are available when existing task has been deleted`() {
    initiateLocalCourse()
    val serverCourse = course {
      lesson("lesson1") {
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    } as EduCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are available when task places are changed`() {
    initiateLocalCourse()
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

  @Test
  fun `test updates are available when new lesson created`() {
    initiateLocalCourse()
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

  @Test
  fun `test updates are not available when lesson update date is changed`() {
    initiateLocalCourse()
    val serverCourse = course {
      lesson("lesson1", updateDate = Date(2 * Time.MINUTE.toLong())) {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    } as EduCourse

    doTestUpdatesAvailable(serverCourse, 0)
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