package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.AdditionalFilesUtils
import com.jetbrains.edu.coursecreator.actions.stepik.hyperskill.GetHyperskillLesson
import com.jetbrains.edu.coursecreator.actions.stepik.hyperskill.PushHyperskillLesson
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.InMemoryBinaryContents
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikTestUtils
import com.jetbrains.edu.learning.stepik.StepikTestUtils.logOutFakeStepikUser
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import org.apache.commons.codec.binary.Base64
import java.io.File

class HyperskillLessonTest : EduTestCase() {
  fun `test collecting course additional files`() {
    val course = courseWithFiles(courseProducer = ::HyperskillCourse, courseMode = CourseMode.EDUCATOR) {
      frameworkLesson {
        eduTask {}
      }
      additionalFile("package.json", "My cool dependencies")
    }
    val info = AdditionalFilesUtils.collectAdditionalLessonInfo(course.lessons.first(), project)

    assertEquals(1, info.additionalFiles.size)
    assertEquals("package.json", info.additionalFiles[0].name)
    assertEquals("My cool dependencies", info.additionalFiles[0].text)
  }

  fun `test push course with binary file`() {
    StepikTestUtils.loginFakeStepikUser()
    val mockConnector = StepikConnector.getInstance() as MockStepikConnector

    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      val responseFileName = when (path) {
        "/api/lessons" -> "lessons_response_278738129.json"
        "/api/step-sources" -> "step-source.json"
        else -> ""
      }
      mockResponse(responseFileName)
    }
    val dbFilePath = "database.db"
    val base64Text = "eAErKUpNVTA3ZjA0MDAzMVHITczM08suYTh0o+NNPdt26bgThdosKRdPVXHN/wNVUpSamJKbqldSUcKwosqLb/75qC5OmZAJs9O9Di0I/PoCAJ5FH4E="
    val course = courseWithFiles("Test Course", courseMode = CourseMode.EDUCATOR, id = 1) {
      frameworkLesson("lesson1") {
        eduTask {
          taskFile("taskFile1.txt")
          taskFile(dbFilePath, InMemoryBinaryContents.parseBase64Encoding(base64Text), false)
        }
      }
    }

    val firstLesson = course.lessons.first()
    val task = firstLesson.taskList.first()
    val taskFile = task.taskFiles["database.db"]
    runWriteAction {
      taskFile!!.getVirtualFile(project)!!.setBinaryContent(Base64.decodeBase64("binary file"))
    }

    UIUtil.dispatchAllInvocationEvents()
    PushHyperskillLesson.doPush(firstLesson, project)
    logOutFakeStepikUser()
  }

  fun `test receiving course with binary files`() {
    val lessonId = 929485
    val stepId = 3885098
    val mockConnector = StepikConnector.getInstance() as MockStepikConnector

    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      val responseFileName = when (path) {
        "/api/lessons?ids%5B%5D=$lessonId" -> "lessons_response_$lessonId.json"
        "/api/steps?ids%5B%5D=$stepId" -> "steps_response_$stepId.json"
        else -> "response_empty.json"
      }
      mockResponse(responseFileName)
    }

    val lessonAttachmentLink = "${StepikNames.getStepikUrl()}/media/attachments/lesson/${lessonId}/${StepikNames.ADDITIONAL_INFO}"
    mockConnector.withAttachments(mapOf(lessonAttachmentLink to FileUtil.loadFile(File(getTestFile("attachments.json")))))

    val course = GetHyperskillLesson.createCourse(lessonId.toString()) ?: error("Failed to get course")
    course.apply {
      languageId = PlainTextLanguage.INSTANCE.id
      initializeCourse(project, this)
      createCourseFiles(project)
    }
    assertInstanceOf(course, HyperskillCourse::class.java)

    val task = course.lessons.first().taskList.first()
    val databaseFile = task.taskFiles["database.db"] ?: error("Failed to get database file")
    val archiveFile = task.taskFiles["file.tar.gz"] ?: error("Failed to get archive file")

    runWriteAction {
      val databaseContent = databaseFile.getVirtualFile(project)!!.loadEncodedContent(false)
      assertEquals("database content", databaseContent)

      val archiveContent = archiveFile.getVirtualFile(project)!!.loadEncodedContent(false)
      assertEquals("archive content", archiveContent)
    }
  }

  fun `test receiving course additional files`() {
    val lessonId = 278738129
    val mockConnector = StepikConnector.getInstance() as MockStepikConnector

    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      val responseFileName = when (path) {
        "/api/lessons?ids%5B%5D=$lessonId" -> "lessons_response_$lessonId.json"
        "/api/steps?ids%5B%5D=111" -> "steps_response_111.json"
        else -> "response_empty.json"
      }
      mockResponse(responseFileName)
    }

    val lessonAttachmentLink = "${StepikNames.getStepikUrl()}/media/attachments/lesson/${lessonId}/${StepikNames.ADDITIONAL_INFO}"
    mockConnector.withAttachments(mapOf(lessonAttachmentLink to FileUtil.loadFile(File(getTestFile("attachments.json")))))

    val course = GetHyperskillLesson.createCourse(lessonId.toString()) ?: error("Failed to get course")
    assertInstanceOf(course, HyperskillCourse::class.java)

    val additionalFiles = course.additionalFiles
    assertEquals(1, additionalFiles.size)
    assertEquals("build.gradle", additionalFiles[0].name)
    assertEquals("additional file text", additionalFiles[0].text)
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/hyperskill/"

  override fun tearDown() {
    try {
      EduSettings.getInstance().user = null
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}