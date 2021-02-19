package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduUtils.getFirstTask
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdater
import com.jetbrains.edu.learning.update.StudentCourseUpdateTest

class MarketplaceCourseUpdateTest : StudentCourseUpdateTest() {
  override val defaultSettings: Unit get() = Unit

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
          file("task.html")
        }
        dir("task2") {
          file("TaskFile2Renamed.kt")
          file("task.html")
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
          file("task.html")
        }
        dir("task2") {
          file("TaskFile2Renamed.kt")
          file("task.html")
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
          file("task.html")
        }
        dir("task2") {
          file("TaskFile2.kt")
          file("task.html")
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
          file("task.html")
        }
        dir("task2") {
          file("TaskFile2.kt")
          file("task.html")
        }
      }
    }

    doTest(course, serverCourse, expectedStructure, 2)
    assertEquals(CheckStatus.Unchecked, getFirstTask(course)?.status)
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
    val course = createCourseFromJson("$testPath/course.json", CourseMode.STUDENT)
    val courseFromServer = createCourseFromJson("$testPath/updated_course.json", CourseMode.STUDENT)
    modifyCourse(course)
    doTest(course, courseFromServer, expectedFileTree, 1)
  }
}