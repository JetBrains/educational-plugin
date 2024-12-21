package com.jetbrains.edu.coursecreator.archive

import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.cipher.AES256Cipher
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSE_CONTENTS_FOLDER
import com.jetbrains.edu.learning.courseFormat.ext.visitEduFiles
import com.jetbrains.edu.learning.json.pathInArchive
import org.junit.Assert
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

abstract class CourseArchiveTestBase : EduActionTestCase() {
  protected fun doTest() {
    val expectedCourseJson = loadExpectedJson()

    val generatedJsonFile = generateJson()
    assertEquals(expectedCourseJson, generatedJsonFile)

    doWithArchiveCreator { creator, course ->
      val out = ByteArrayOutputStream()
      creator.doCreateCourseArchive(CourseArchiveIndicator(), course, out)
      val zip = out.toByteArray()

      val fileName2contents = mutableMapOf<String, ByteArray>()
      ZipInputStream(ByteArrayInputStream(zip)).use { zipIn ->
        while (true) {
          val entry = zipIn.nextEntry ?: break
          if (!entry.name.startsWith("$COURSE_CONTENTS_FOLDER/")) continue
          fileName2contents[entry.name] = zipIn.readAllBytes()
        }
      }

      var eduFilesCount = 0
      course.visitEduFiles { eduFile ->
        val actualEncryptedContents = fileName2contents[eduFile.pathInArchive] ?: error("File ${eduFile.name} not found in archive")
        val actualContents = AES256Cipher.TEST_CIPHER.decrypt(actualEncryptedContents)

        val expectedContents = when (val contents = eduFile.contents) {
          is BinaryContents -> contents.bytes
          is TextualContents -> contents.text.toByteArray()
          is UndeterminedContents -> error("unexpected undetermined contents")
        }

        Assert.assertArrayEquals(expectedContents, actualContents)

        eduFilesCount++
      }
      assertEquals("Number of files in archive must be the same as in the course", eduFilesCount, fileName2contents.size)
    }
  }

  private fun loadExpectedJson(): String {
    val fileName = getTestFile()
    val jsonFromFile = FileUtil.loadFile(File(testDataPath, fileName))
    val withLastJsonVersion = jsonFromFile.replace(""""version"\s*:\s*<last version>""".toRegex(), """"version" : $JSON_FORMAT_VERSION""")
    return withLastJsonVersion
  }

  private fun <R> doWithArchiveCreator(action: (archiveCreator: CourseArchiveCreator, preparedCourse: Course) -> R): R {
    val course = StudyTaskManager.getInstance(project).course ?: error("No course found")

    val copiedCourse = course.copy()
    copiedCourse.authors = course.authors

    val creator = getArchiveCreator()
    creator.prepareCourse(copiedCourse)

    return action(creator, copiedCourse)
  }

  protected fun generateJson(): String = doWithArchiveCreator { creator, course ->
    val mapper = creator.getMapper(course)
    val json = mapper.writer(printer).writeValueAsString(course)
    StringUtilRt.convertLineSeparators(json).replace("\\n\\n".toRegex(), "\n")
  }

  protected open fun getArchiveCreator(
    location: String = "${myFixture.project.basePath}/${CCUtils.GENERATED_FILES_FOLDER}/course.zip"
  ): CourseArchiveCreator = CourseArchiveCreator(myFixture.project, location, AES256Cipher.TEST_CIPHER)

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
