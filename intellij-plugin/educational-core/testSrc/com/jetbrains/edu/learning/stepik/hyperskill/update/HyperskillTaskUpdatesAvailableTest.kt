package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.intellij.util.Time
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.stepik.hyperskill.init
import com.jetbrains.edu.learning.update.UpdatesAvailableTestBase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.*

class HyperskillTaskUpdatesAvailableTest : UpdatesAvailableTestBase<HyperskillCourse>() {
  private fun doTestUpdatesAvailable(remoteCourse: HyperskillCourse, expectedAmountOfUpdates: Int) {
    createCourseStructure(localCourse)
    val updater = HyperskillTaskUpdater(project, localCourse.lessons.first())
    val updates = runBlocking {
      updater.collect(remoteCourse.lessons.first())
    }
    assertEquals(expectedAmountOfUpdates, updates.size)
  }

  @Test
  fun `test updates are available when task name is changed`() {
    initiateLocalCourse()
    val serverCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
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

  @Test
  fun `test updates are available when description text is changed`() {
    initiateLocalCourse()
    val serverCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text is updated")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text is updated")
        }
      }
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 2)
  }

  @Test
  fun `test updates are available when placeholders are changed`() {
    localCourse = course(courseProducer = ::HyperskillCourse) {
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
    } as HyperskillCourse
    localCourse.init(1, false)

    val serverCourse = course(courseProducer = ::HyperskillCourse) {
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
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are available when the task changes its type`() {
    initiateLocalCourse()
    val serverCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
        codeTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are available when taskFile name is changed`() {
    initiateLocalCourse()
    val serverCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2Renamed.kt", "task file 2 text")
        }
      }
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are available when amount of taskFiles is changed`() {
    initiateLocalCourse()
    val serverCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
        eduTask("task1", stepId = 1) {
          taskFile("TaskFile1.kt", "task file 1 text")
        }
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
          taskFile("TaskFile3.kt", "task file 3 text")
        }
      }
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are available when unsupported task became supported`() {
    localCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
        unsupportedTask("task1", stepId = 1)
        eduTask("task2", stepId = 2) {
          taskFile("TaskFile2.kt", "task file 2 text")
        }
      }
    } as HyperskillCourse
    localCourse.init(1, false)

    val serverCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
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

  @Test
  fun `test updates are available when choice options for ChoiceTask are changed`() {
    localCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
        choiceTask(
          "task1", stepId = 1, choiceOptions = mapOf(
            "Option1" to ChoiceOptionStatus.UNKNOWN,
            "Option2" to ChoiceOptionStatus.UNKNOWN,
            "Option3" to ChoiceOptionStatus.UNKNOWN
          )
        )
        choiceTask(
          "task2", stepId = 2, choiceOptions = mapOf(
            "Option1" to ChoiceOptionStatus.UNKNOWN,
            "Option2" to ChoiceOptionStatus.UNKNOWN,
            "Option3" to ChoiceOptionStatus.UNKNOWN
          )
        )
      }
    } as HyperskillCourse
    localCourse.init(1, false)

    val serverCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
        choiceTask(
          "task1", stepId = 1, choiceOptions = mapOf(
            "NewOption1" to ChoiceOptionStatus.UNKNOWN,
            "NewOption2" to ChoiceOptionStatus.UNKNOWN,
            "NewOption3" to ChoiceOptionStatus.UNKNOWN
          )
        )
        choiceTask(
          "task2", stepId = 2, choiceOptions = mapOf(
            "Option1" to ChoiceOptionStatus.UNKNOWN,
            "Option2" to ChoiceOptionStatus.UNKNOWN,
            "Option3" to ChoiceOptionStatus.UNKNOWN
          )
        )
      }
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are available when options for SortingTask are changed`() {
    localCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
        sortingTask("task1", stepId = 1, options = listOf("0", "1", "2"))
        sortingTask("task2", stepId = 2, options = listOf("0", "1", "2"))
      }
    } as HyperskillCourse
    localCourse.init(1, false)

    val serverCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
        sortingTask("task1", stepId = 1, options = listOf("0", "1", "2"))
        sortingTask("task2", stepId = 2, options = listOf("3", "4", "5"))
      }
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are available when options or captions for MatchingTask are changed`() {
    localCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
        matchingTask("task1", stepId = 1, options = listOf("0", "1", "2"), captions = listOf("A", "B", "C"))
        matchingTask("task2", stepId = 2, options = listOf("0", "1", "2"), captions = listOf("A", "B", "C"))
      }
    } as HyperskillCourse
    localCourse.init(1, false)

    val serverCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
        matchingTask("task1", stepId = 1, options = listOf("3", "4", "5"), captions = listOf("A", "B", "C"))
        matchingTask("task2", stepId = 2, options = listOf("0", "1", "2"), captions = listOf("D", "E", "F"))
      }
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 2)
  }

  @Test
  fun `test updates are available when checkProfile for RemoteEduTask is changed`() {
    localCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
        remoteEduTask("task1", stepId = 1, checkProfile = "profile 1")
        remoteEduTask("task2", stepId = 2, checkProfile = "profile 2")
      }
    } as HyperskillCourse
    localCourse.init(1, false)

    val serverCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
        remoteEduTask("task1", stepId = 1, checkProfile = "profile 1")
        remoteEduTask("task2", stepId = 2, checkProfile = "profile 2 updated")
      }
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are available when new task created`() {
    initiateLocalCourse()
    val serverCourse = course(courseProducer = ::HyperskillCourse) {
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
    } as HyperskillCourse

    doTestUpdatesAvailable(serverCourse, 1)
  }

  @Test
  fun `test updates are available when task update date is changed`() {
    initiateLocalCourse()
    val serverCourse = course(courseProducer = ::HyperskillCourse) {
      lesson {
        eduTask("task1", stepId = 1, updateDate = Date(2 * Time.MINUTE.toLong())) {
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
      lesson {
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