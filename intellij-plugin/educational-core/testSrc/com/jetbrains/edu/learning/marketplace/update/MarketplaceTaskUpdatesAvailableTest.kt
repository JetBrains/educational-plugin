package com.jetbrains.edu.learning.marketplace.update

import com.intellij.util.Time
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.update.UpdatesAvailableTestBase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.*

class MarketplaceTaskUpdatesAvailableTest : UpdatesAvailableTestBase<EduCourse>() {
  private fun doTestUpdatesAvailable(remoteCourse: EduCourse, expectedAmountOfUpdates: Int) {
    createCourseStructure(localCourse)
    val updater = MarketplaceTaskUpdater(project, localCourse.lessons.first())
    val updates = runBlocking {
      updater.collect(remoteCourse.lessons.first())
    }
    assertEquals(expectedAmountOfUpdates, updates.size)
  }

  @Test
  fun `test updates are available when task name is changed`() {
    initiateLocalCourse()
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

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are available when description text is changed`() {
    initiateLocalCourse()
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

    doTestUpdatesAvailable(serverCourse, 2)
  }

  @Test
  fun `test updates are available when placeholders are changed`() {
    localCourse = course {
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

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are available when the task changes its type`() {
    initiateLocalCourse()
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

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are available when taskFile name is changed`() {
    initiateLocalCourse()
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

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are available when amount of taskFiles is changed`() {
    initiateLocalCourse()
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

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are available when new task created`() {
    initiateLocalCourse()
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

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are not available when task update date is changed`() {
    initiateLocalCourse()
    val serverCourse = course {
      lesson {
        eduTask("task1", stepId = 1, updateDate = Date(2 * Time.MINUTE.toLong())) {
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
      lesson {
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