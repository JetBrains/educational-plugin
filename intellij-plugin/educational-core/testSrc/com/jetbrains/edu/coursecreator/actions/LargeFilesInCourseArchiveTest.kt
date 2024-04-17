package com.jetbrains.edu.coursecreator.actions

import com.fasterxml.jackson.databind.JsonMappingException
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.EDU_TEST_BIN
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.InMemoryBinaryContents
import com.jetbrains.edu.learning.courseFormat.getBinaryFileLimit
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import org.junit.Test

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
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("task.$EDU_TEST_BIN", InMemoryBinaryContents(ByteArray(FileUtilRt.LARGE_FOR_CONTENT_LOADING + 1)))
        }
      }
    }

    assertThrows(HugeBinaryFileException::class.java, StringUtil.formatFileSize(FileUtilRt.LARGE_FOR_CONTENT_LOADING.toLong())) {
      try {
        generateJson()
      }
      catch (e: JsonMappingException) {
        throw e.cause!!
      }
    }
  }

  @Test
  fun `test large binary additional file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      additionalFile("file.$EDU_TEST_BIN", InMemoryBinaryContents(ByteArray(FileUtilRt.LARGE_FOR_CONTENT_LOADING + 1)))
    }

    assertThrows(HugeBinaryFileException::class.java, StringUtil.formatFileSize(FileUtilRt.LARGE_FOR_CONTENT_LOADING.toLong())) {
      try {
        generateJson()
      }
      catch (e: JsonMappingException) {
        throw e.cause!!
      }
    }
  }
}