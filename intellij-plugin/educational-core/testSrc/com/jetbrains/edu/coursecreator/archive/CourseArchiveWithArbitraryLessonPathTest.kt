package com.jetbrains.edu.coursecreator.archive

import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.junit.Test

class CourseArchiveWithArbitraryLessonPathTest : CourseArchiveTestBase() {
  override fun getTestDataPath(): String = super.getTestDataPath() + "/archive/createCourseArchive"

  @Test
  fun `test archive with arbitrary lesson path`() {
    val course = courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      shift = "some/arbitrary/path/"
    ) {
      lesson("lesson1") {
        theoryTask("task1") {
          taskFile("Main.kt")
        }
        eduTask("task2") {
          taskFile("Main.kt")
        }
      }
      lesson("lesson2") {
        theoryTask("task1") {
          taskFile("Main.kt")
        }
        eduTask("task2") {
          taskFile("Main.kt")
        }
      }
    }

    doTest(course)
  }
}