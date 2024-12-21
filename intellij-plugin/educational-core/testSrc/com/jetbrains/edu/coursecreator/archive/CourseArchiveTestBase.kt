package com.jetbrains.edu.coursecreator.archive

import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.cipher.Cipher
import com.jetbrains.edu.learning.cipher.NoOpCipher
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSE_CONTENTS_FOLDER
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSE_META_FILE
import com.jetbrains.edu.learning.courseFormat.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.copy
import org.junit.Assert
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

abstract class CourseArchiveTestBase : EduActionTestCase() {
  protected fun doTest(cipher: Cipher = NoOpCipher()) {
    val expectedCourseJson = loadExpectedJson()

    val generatedJsonFile = generateJson(cipher)
    assertEquals(expectedCourseJson, generatedJsonFile)

    doWithArchiveCreator(cipher) { creator, course ->
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

      val expectedDataDir = getExpectedDataDirectory()
      for (file in expectedDataDir.walkTopDown()) {
        if (file.isDirectory) continue

        val relativePath = file.relativeTo(expectedDataDir).invariantSeparatorsPath
        // we've already checked course.json above
        // TODO: always check course.json together with content
        if (relativePath == COURSE_META_FILE) continue

        val actualContent = fileName2contents.remove(relativePath) ?: kotlin.test.fail("File $relativePath not found in archive")
        val expectedContent = file.readBytes()
        Assert.assertArrayEquals(expectedContent, actualContent)
      }

      if (fileName2contents.isNotEmpty()) {
        fail("Unexpected files in archive: ${fileName2contents.keys}")
      }
    }
  }

  protected fun loadExpectedJson(): String {
    val jsonFile = getExpectedDataDirectory().resolve(COURSE_META_FILE)
    val jsonFromFile = FileUtil.loadFile(jsonFile)
    val withLastJsonVersion = jsonFromFile.replace(""""version"\s*:\s*<last version>""".toRegex(), """"version" : $JSON_FORMAT_VERSION""")
    return withLastJsonVersion
  }

  private fun <R> doWithArchiveCreator(cipher: Cipher = NoOpCipher(), action: (archiveCreator: CourseArchiveCreator, preparedCourse: Course) -> R): R {
    val course = StudyTaskManager.getInstance(project).course ?: error("No course found")

    val copiedCourse = course.copy()
    copiedCourse.authors = course.authors

    val creator = getArchiveCreator(cipher = cipher)
    creator.prepareCourse(copiedCourse)

    return action(creator, copiedCourse)
  }

  protected fun generateJson(cipher: Cipher = NoOpCipher()): String = doWithArchiveCreator(cipher) { creator, course ->
    val mapper = creator.getMapper(course)
    val json = mapper.writer(printer).writeValueAsString(course)
    StringUtilRt.convertLineSeparators(json).replace("\\n\\n".toRegex(), "\n")
  }

  protected open fun getArchiveCreator(
    location: String = "${myFixture.project.basePath}/${CCUtils.GENERATED_FILES_FOLDER}/course.zip",
    cipher: Cipher = NoOpCipher()
  ): CourseArchiveCreator = CourseArchiveCreator(myFixture.project, location, cipher)

  private fun getExpectedDataDirectory(): File {
    return File(testDataPath, getTestName(true).trim())
  }

  private val printer: PrettyPrinter
    get() {
      val prettyPrinter = DefaultPrettyPrinter()
      prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
      return prettyPrinter
    }
}
