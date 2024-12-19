package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.EDU_TEST_BIN
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.InMemoryBinaryContents
import com.jetbrains.edu.learning.courseFormat.getBinaryFileLimit
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import com.jetbrains.edu.learning.json.encrypt.AES256Cipher
import com.jetbrains.edu.learning.json.encrypt.TEST_AES_KEY
import org.junit.Test
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.test.assertContains

class LargeFilesInCourseArchiveTest : CourseArchiveTestBase() {

  @Test
  fun `test large binary file in a framework lesson`() {

    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson {
        eduTask {
          taskFile("task.$EDU_TEST_BIN", InMemoryBinaryContents(ByteArray(getBinaryFileLimit() + 1)))
        }
      }
    }

    assertThrows(HugeBinaryFileException::class.java, StringUtil.formatFileSize(getBinaryFileLimit().toLong())) {
      generateJson()
    }
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
    val fileSizeMessage = StringUtil.formatFileSize(FileUtilRt.LARGE_FOR_CONTENT_LOADING.toLong())
    val tempFileForArchive = Files.createTempFile("test-course-archive-", ".zip")
    try {
      val archiveCreator = CourseArchiveCreator(project, tempFileForArchive.absolutePathString(), AES256Cipher(TEST_AES_KEY))
      val errorMessage = archiveCreator.createArchive(course) ?: kotlin.test.fail("Must generate an error message")
      assertContains(
        errorMessage,
        fileSizeMessage,
        false,
        "The error message must contain the information that there was a huge file in archive"
      )
    }
    finally {
      tempFileForArchive.deleteIfExists()
    }
  }
}