package com.jetbrains.edu.coursecreator.actions

import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import java.io.File

abstract class CourseArchiveTestBase : EduActionTestCase() {
  fun `test large binary file`() {
    val fileName = "task.${EduUtils.EDU_TEST_BIN}"
    var placeholder = "placeholder"

    while (placeholder.toByteArray(Charsets.UTF_8).size <= EduUtils.getBinaryFileLimit()) {
      placeholder += placeholder
    }

    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson {
        eduTask {
          taskFile(fileName, placeholder)
        }
      }
    }

    assertThrows(HugeBinaryFileException::class.java) {
      generateJson()
    }
  }

  protected fun doTest() {
    val generatedJsonFile = generateJson()
    val expectedCourseJson = loadExpectedJson()
    assertEquals(expectedCourseJson, generatedJsonFile)
  }

  private fun loadExpectedJson(): String {
    val fileName = getTestFile()
    return FileUtil.loadFile(File(testDataPath, fileName))
  }

  private fun generateJson(): String {
    val course = StudyTaskManager.getInstance(project).course ?: error("No course found")
    val creator = getArchiveCreator()
    creator.prepareCourse(course)
    val mapper = creator.getMapper(course)
    val json = mapper.writer(printer).writeValueAsString(course)
    return StringUtilRt.convertLineSeparators(json).replace("\\n\\n".toRegex(), "\n")
  }

  abstract fun getArchiveCreator() : CourseArchiveCreator

  private fun getTestFile(): String {
    return getTestName(true).trim() + ".json"
  }

  private val printer: PrettyPrinter
    get() {
      val prettyPrinter = DefaultPrettyPrinter()
      prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
      return prettyPrinter
    }
}
