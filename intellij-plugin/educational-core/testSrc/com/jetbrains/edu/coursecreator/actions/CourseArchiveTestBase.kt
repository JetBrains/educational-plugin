package com.jetbrains.edu.coursecreator.actions

import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.EduExperimentalFeatures.COURSE_FORMAT_WITH_FILES_OUTSIDE_JSON
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.copy
import com.jetbrains.edu.learning.json.encrypt.TEST_AES_KEY
import com.jetbrains.edu.learning.withFeature
import java.io.File

abstract class CourseArchiveTestBase : EduActionTestCase() {
  protected fun doTest() {
    val expectedCourseJson = loadExpectedJson()

    withFeature(COURSE_FORMAT_WITH_FILES_OUTSIDE_JSON, false) {
      val generatedJsonFile = generateJson()
      assertEquals(expectedCourseJson, generatedJsonFile)
    }

    withFeature(COURSE_FORMAT_WITH_FILES_OUTSIDE_JSON, true) {
      // TODO: when COURSE_FORMAT_WITH_FILES_OUTSIDE_JSON feature is removed remove "text" fields from test jsons
      val expectedCourseJsonVersion19 = expectedCourseJson
        .replace(""""version" : \d{1,2}""".toRegex(), """"version" : 19""")
        .replace("""^\s*"text" : ".{2,}",\n""".toRegex(RegexOption.MULTILINE), "")

      val generatedJsonFile = generateJson()
      assertEquals(expectedCourseJsonVersion19, generatedJsonFile)
    }
  }

  private fun loadExpectedJson(): String {
    val fileName = getTestFile()
    return FileUtil.loadFile(File(testDataPath, fileName))
  }

  protected fun generateJson(): String {
    val course = StudyTaskManager.getInstance(project).course ?: error("No course found")

    // TODO: when COURSE_FORMAT_WITH_FILES_OUTSIDE_JSON is removed, do not copy the course

    val copiedCourse = course.copy()
    copiedCourse.authors = course.authors

    val creator = getArchiveCreator()
    creator.prepareCourse(copiedCourse)
    val mapper = creator.getMapper(copiedCourse)
    val json = mapper.writer(printer).writeValueAsString(copiedCourse)
    return StringUtilRt.convertLineSeparators(json).replace("\\n\\n".toRegex(), "\n")
  }

  protected open fun getArchiveCreator(
    location: String = "${myFixture.project.basePath}/${CCUtils.GENERATED_FILES_FOLDER}/course.zip"
  ): CourseArchiveCreator = CourseArchiveCreator(myFixture.project, location, TEST_AES_KEY)

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
