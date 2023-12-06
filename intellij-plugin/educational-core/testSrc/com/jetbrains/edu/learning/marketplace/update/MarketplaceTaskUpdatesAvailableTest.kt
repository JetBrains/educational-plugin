package com.jetbrains.edu.learning.marketplace.update

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import kotlinx.coroutines.runBlocking

class MarketplaceTaskUpdatesAvailableTest : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  override fun runInDispatchThread(): Boolean = false

  private fun doTestUpdatesAvailable(localCourse: EduCourse, remoteCourse: EduCourse, expectedAmountOfUpdates: Int) {
    createCourseStructure(localCourse)
    val updater = MarketplaceTaskUpdater(project, localCourse.lessons.first())
    runBlocking {
      updater.collect(remoteCourse.lessons.first())
    }
    assertEquals(expectedAmountOfUpdates, updater.amountOfUpdates)
  }

  fun `test updates are available when task name is changed`() {
    val course = createCourse()

    val serverCourse = course {
      lesson {
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

  fun `test updates are available when description text is changed`() {
    val course = createCourse()
    val serverCourse = course {
      lesson {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text is updated")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text is updated")
        }
      }
    } as EduCourse

    doTestUpdatesAvailable(course, serverCourse, 2)
  }

  fun `test updates are available when placeholders are changed`() {
    val course = course {
      lesson {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "fun foo() { <p>TODO</p>() }") {
            placeholder(index = 0, placeholderText = "TODO")
          }
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    }.apply { marketplaceCourseVersion = 1 } as EduCourse

    val serverCourse = course {
      lesson {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "fun foo() { <p>TODO()</p> }") {
            placeholder(index = 0, placeholderText = "TODO()")
          }
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    } as EduCourse

    doTestUpdatesAvailable(course, serverCourse, 1)
  }

  fun `test updates are available when the task changes its type`() {
    val course = createCourse()

    val serverCourse = course {
      lesson {
        codeTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    } as EduCourse

    doTestUpdatesAvailable(course, serverCourse, 1)
  }

  fun `test updates are available when taskFile name is changed`() {
    val course = createCourse()

    val serverCourse = course {
      lesson {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2Renamed.kt", "task file 2 text")
        }
      }
    } as EduCourse

    doTestUpdatesAvailable(course, serverCourse, 1)
  }

  fun `test updates are available when amount of taskFiles is changed`() {
    val course = createCourse()

    val serverCourse = course {
      lesson {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
          taskFile("TaskFile3.kt", "task file 3 text")
        }
      }
    } as EduCourse

    doTestUpdatesAvailable(course, serverCourse, 1)
  }

  fun `test updates are available when new task created`() {
    val course = createCourse()

    val serverCourse = course {
      lesson {
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

  private fun createCourse(): EduCourse {
    val course = course {
      lesson {
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