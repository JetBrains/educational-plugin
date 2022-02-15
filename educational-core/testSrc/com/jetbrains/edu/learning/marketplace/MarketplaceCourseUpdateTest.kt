package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduUtils.getFirstTask
import com.jetbrains.edu.learning.FileTree
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.createCourseFromJson
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdater
import com.jetbrains.edu.learning.update.CourseUpdateTestBase
import com.jetbrains.rd.util.firstOrNull
import java.util.*

class MarketplaceCourseUpdateTest : CourseUpdateTestBase() {
  override val defaultSettings: Unit get() = Unit

  fun `test update date updated`() {
    val course = createCourse(CheckStatus.Solved).apply { updateDate = Date(1619697473000) }

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("TaskFile1.kt")
        }
        eduTask(stepId = 2) {
          taskFile("TaskFile2Renamed.kt")
        }
      }
    }.apply { updateDate = Date(1624354026000) } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("TaskFile1.kt")
          file("task.md")
        }
        dir("task2") {
          file("TaskFile2Renamed.kt")
          file("task.md")
        }
      }
    }

    doTest(course, serverCourse, expectedStructure, 2)
    assertEquals(course.updateDate, serverCourse.updateDate)
  }

  fun `test save task status Solved if task not updated`() {
    val course = createCourse(CheckStatus.Solved)

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("TaskFile1.kt")
        }
        eduTask(stepId = 2) {
          taskFile("TaskFile2Renamed.kt")
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("TaskFile1.kt")
          file("task.md")
        }
        dir("task2") {
          file("TaskFile2Renamed.kt")
          file("task.md")
        }
      }
    }

    doTest(course, serverCourse, expectedStructure, 2)
    assertEquals(CheckStatus.Solved, getFirstTask(course)?.status)
  }

  fun `test save task status Failed if task not updated`() {
    val course = createCourse(CheckStatus.Failed)

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("TaskFile1.kt")
        }
        eduTask(stepId = 2) {
          taskFile("TaskFile2Renamed.kt")
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("TaskFile1.kt")
          file("task.md")
        }
        dir("task2") {
          file("TaskFile2Renamed.kt")
          file("task.md")
        }
      }
    }

    doTest(course, serverCourse, expectedStructure, 2)
    assertEquals(CheckStatus.Failed, getFirstTask(course)?.status)
  }

  fun `test save task status Solved if task was updated`() {
    val course = createCourse(CheckStatus.Solved)

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("TaskFile1Renamed.kt")
        }
        eduTask(stepId = 2) {
          taskFile("TaskFile2.kt")
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("TaskFile1Renamed.kt")
          file("task.md")
        }
        dir("task2") {
          file("TaskFile2.kt")
          file("task.md")
        }
      }
    }

    doTest(course, serverCourse, expectedStructure, 2)
    assertEquals(CheckStatus.Solved, getFirstTask(course)?.status)
  }

  fun `test do not save task status Failed if task was updated`() {
    val course = createCourse(CheckStatus.Failed)

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("TaskFile1Renamed.kt")
        }
        eduTask(stepId = 2) {
          taskFile("TaskFile2.kt")
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("TaskFile1Renamed.kt")
          file("task.md")
        }
        dir("task2") {
          file("TaskFile2.kt")
          file("task.md")
        }
      }
    }

    doTest(course, serverCourse, expectedStructure, 2)
    assertEquals(CheckStatus.Unchecked, getFirstTask(course)?.status)
  }

  fun `test placeholder possible answer changed`() {
    val course = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("Buzz.kt", "fun bar(): String = <p>TODO()</p>") {
            placeholder(0, "\"Bar\"")
          }
        }
      }
    } as EduCourse
    course.marketplaceCourseVersion = 1

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("Buzz.kt", "fun bar(): String = <p>TODO()</p>") {
            placeholder(0, "\"Updated\"")
          }
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("Buzz.kt")
          file("task.md")
        }
      }
    }

    doTest(course, serverCourse, expectedStructure, 2)
    val placeholder = getFirstTask(course)?.taskFiles?.firstOrNull()?.value?.answerPlaceholders?.firstOrNull()
    checkNotNull(placeholder)
    assertEquals("\"Updated\"", placeholder.possibleAnswer)
  }

  fun `test placeholder length changed`() {
    val course = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("Buzz.kt", "fun bar(): String = <p>TODO()</p>") {
            placeholder(0, "\"Bar\"")
          }
        }
      }
    } as EduCourse
    course.marketplaceCourseVersion = 1

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("Buzz.kt", "fun bar(): String = <p>TODO</p>()") {
            placeholder(0, "\"Updated\"")
          }
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("Buzz.kt")
          file("task.md")
        }
      }
    }

    doTest(course, serverCourse, expectedStructure, 2)
    val placeholder = getFirstTask(course)?.taskFiles?.firstOrNull()?.value?.answerPlaceholders?.firstOrNull()
    checkNotNull(placeholder)
    assertEquals(4, placeholder.length)
  }

  fun `test placeholder dependency changed`() {
    val course = course {
      lesson {
        eduTask(stepId = 1, name = "task1") {
          taskFile("TaskFile1.kt", "fn foo() = <p>TODO()</p>") {
            placeholder(0, "123")
          }
        }
        eduTask(stepId = 2, name = "task2") {
          taskFile("TaskFile2.kt", "fn foo() = <p>TODO()</p>") {
            placeholder(0, "123")
          }
        }
        eduTask(stepId = 3, name = "task3") {
          taskFile("Buzz.kt", "fun bar(): String = <p>TODO()</p>") {
            placeholder(0, "123", dependency = "lesson1#task1#TaskFile1.kt#1")
          }
        }
      }
    } as EduCourse
    course.marketplaceCourseVersion = 1

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1, name = "task1") {
          taskFile("TaskFile1.kt", "fn foo() = <p>TODO()</p>") {
            placeholder(0, "123")
          }
        }
        eduTask(stepId = 2, name = "task2") {
          taskFile("TaskFile2.kt", "fn foo() = <p>TODO()</p>") {
            placeholder(0, "123")
          }
        }
        eduTask(stepId = 3, name = "task3") {
          taskFile("Buzz.kt", "fun bar(): String = <p>TODO()</p>") {
            placeholder(0, "123", dependency = "lesson1#task2#TaskFile2.kt#1")
          }
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("TaskFile1.kt")
          file("task.md")
        }
        dir("task2") {
          file("TaskFile2.kt")
          file("task.md")
        }
        dir("task3") {
          file("Buzz.kt")
          file("task.md")
        }
      }
    }

    doTest(course, serverCourse, expectedStructure, 2)
    val placeholder = course.allTasks[2].taskFiles?.firstOrNull()?.value?.answerPlaceholders?.firstOrNull()
    checkNotNull(placeholder)
    assertEquals("lesson1#task2#TaskFile2.kt#1", placeholder.placeholderDependency.toString())
  }

  private fun createCourse(firstTaskStatus: CheckStatus): EduCourse {
    val course = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("TaskFile1.kt")
        }
        eduTask(stepId = 2) {
          taskFile("TaskFile2.kt")
        }
      }
    } as EduCourse
    getFirstTask(course)?.status = firstTaskStatus
    course.marketplaceCourseVersion = 1

    return course
  }

  fun doTest(course: EduCourse, courseFromServer: EduCourse, expectedFileTree: FileTree, remoteCourseVersion: Int) {
    loadCourseStructure(course, courseFromServer)
    MarketplaceCourseUpdater(project, course, remoteCourseVersion).doUpdate(courseFromServer)
    checkCourseStructure(course, courseFromServer, expectedFileTree)
    assertEquals(remoteCourseVersion, course.marketplaceCourseVersion)
  }

  override fun doTest(expectedFileTree: FileTree, testPath: String, modifyCourse: (course: EduCourse) -> Unit) {
    val course = createCourseFromJson("$testPath/course.json", CourseMode.STUDY)
    val courseFromServer = createCourseFromJson("$testPath/updated_course.json", CourseMode.STUDY)
    modifyCourse(course)
    doTest(course, courseFromServer, expectedFileTree, 1)
  }
}