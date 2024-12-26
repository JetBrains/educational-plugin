package com.jetbrains.edu.coursecreator.archive

import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.edu.learning.EDU_TEST_BIN
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.InMemoryBinaryContents
import com.jetbrains.edu.learning.courseFormat.getBinaryFileLimit
import org.junit.Test
import kotlin.test.assertIs

class LargeFilesInCourseArchiveTest : CourseArchiveTestBase() {

  @Test
  fun `test large binary file in a framework lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson {
        eduTask {
          taskFile("task.$EDU_TEST_BIN", InMemoryBinaryContents(ByteArray(getBinaryFileLimit() + 1)))
        }
      }
    }

    testHugeBinaryFileInCourseArchive(course)
  }

  @Test
  fun `test large binary file in a non-framework lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("task.$EDU_TEST_BIN", InMemoryBinaryContents(ByteArray(FileUtilRt.LARGE_FOR_CONTENT_LOADING + 1)))
        }
      }
    }

    testHugeBinaryFileInCourseArchive(course)
  }

  @Test
  fun `test large binary additional file`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      additionalFile("file.$EDU_TEST_BIN", InMemoryBinaryContents(ByteArray(FileUtilRt.LARGE_FOR_CONTENT_LOADING + 1)))
    }

    testHugeBinaryFileInCourseArchive(course)
  }

  private fun testHugeBinaryFileInCourseArchive(course: Course) {
    val result = createCourseArchive(course)
    assertIs<Err<*>>(result, "Course creation must generate an error message")
    assertIs<CourseArchiveCreator.HugeBinaryFileError>(result.error)
  }
}