package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCUtils.collectAdditionalLessonInfo
import com.jetbrains.edu.coursecreator.actions.stepik.hyperskill.GetHyperskillLesson
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import java.io.File

class HyperskillLessonTest : EduTestCase() {
  fun `test collecting course additional files`() {
    val course = courseWithFiles(courseProducer = ::HyperskillCourse, courseMode = CCUtils.COURSE_MODE) {
      frameworkLesson {
        eduTask {}
      }
      additionalFile("package.json", "My cool dependencies")
    }
    val info = collectAdditionalLessonInfo(course.lessons.first(), project)

    assertEquals(1, info.additionalFiles.size)
    assertEquals("package.json", info.additionalFiles[0].name)
    assertEquals("My cool dependencies", info.additionalFiles[0].text)
  }

  fun `test receiving course additional files`() {
    val lessonId = 278738129
    val mockConnector = StepikConnector.getInstance() as MockStepikConnector

    mockConnector.withResponseHandler(testRootDisposable) { request ->
      val responseFileName = when (request.path) {
        "/api/lessons?ids%5B%5D=$lessonId" -> "lessons_response_$lessonId.json"
        "/api/steps?ids%5B%5D=111" -> "steps_response_111.json"
        else -> "response_empty.json"
      }
      MockResponseFactory.fromFile(getTestFile(responseFileName))
    }

    val lessonAttachmentLink = "${StepikNames.STEPIK_URL}/media/attachments/lesson/${lessonId}/${StepikNames.ADDITIONAL_INFO}"
    mockConnector.withAttachments(mapOf(lessonAttachmentLink to FileUtil.loadFile(File(getTestFile("attachments.json")))))

    val course = GetHyperskillLesson().createCourse(lessonId.toString()) ?: error("Failed to get course")
    assertInstanceOf(course, HyperskillCourse::class.java)

    val additionalFiles = course.additionalFiles
    assertEquals(1, additionalFiles.size)
    assertEquals("build.gradle", additionalFiles[0].name)
    assertEquals("additional file text", additionalFiles[0].text)
  }

  private fun getTestFile(fileName: String) = testDataPath + fileName

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/hyperskill/"
}