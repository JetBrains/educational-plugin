package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.coursecreator.AdditionalFilesUtils
import com.jetbrains.edu.coursecreator.actions.stepik.hyperskill.GetHyperskillLesson
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
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

  fun `test receiving course additional files`() {
    val lessonId = 278738129
    val mockConnector = StepikConnector.getInstance() as MockStepikConnector

    mockConnector.withResponseHandler(testRootDisposable) { request ->
      val responseFileName = when (request.path) {
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
}