package com.jetbrains.edu.learning.marketplace.update

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import kotlinx.coroutines.runBlocking

class MarketplaceLessonUpdatesAvailableTest : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  override fun runInDispatchThread(): Boolean = false

  @Suppress("SameParameterValue")
  private fun doTestUpdatesAvailable(localCourse: EduCourse, remoteCourse: EduCourse, expectedAmountOfUpdates: Int) {
    createCourseStructure(localCourse)
    val updater = MarketplaceLessonUpdater(project, localCourse)
    runBlocking {
      updater.collect(remoteCourse)
    }
    assertEquals(expectedAmountOfUpdates, updater.amountOfUpdates)
  }

  fun `test updates are available when task name is changed`() {
    val course = createCourse()
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

    doTestUpdatesAvailable(course, serverCourse, 1)
  }

  fun `test updates are available when new task created`() {
    val course = createCourse()
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

    doTestUpdatesAvailable(course, serverCourse, 1)
  }

  fun `test updates are available when existing task has been deleted`() {
    val course = createCourse()
    val serverCourse = course {
      lesson("lesson1") {
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    } as EduCourse

    doTestUpdatesAvailable(course, serverCourse, 1)
  }

  fun `test updates are available when task places are changed`() {
    val course = createCourse()
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

    doTestUpdatesAvailable(course, serverCourse, 1)
  }

  fun `test updates are available when new lesson created`() {
    val course = createCourse()
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

    doTestUpdatesAvailable(course, serverCourse, 1)
  }

  private fun createCourse(): EduCourse {
    val course = course {
      lesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    }.apply { marketplaceCourseVersion = 1 } as EduCourse
    return course
  }
}