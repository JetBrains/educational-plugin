package com.jetbrains.edu.learning.marketplace

import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.marketplace.api.MarketplaceStateOnClose
import com.jetbrains.edu.learning.marketplace.api.MarketplaceStateOnClosePost
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import org.junit.Test
import java.util.*

class MarketplaceCreateStateOnCloseTest : EduTestCase() {
  private val course: EduCourse by lazy {
    courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::EduCourse) {
      section("Section") {
        lesson("Lesson") {
          eduTask("Edu problem", stepId = 1) {
            taskFile("src/Task.kt", "solution file text")
            taskFile("src/Test.kt", "test file text", visible = false)
          }
        }
      }
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 3
    } as EduCourse
  }

  private val objectMapper by lazy {
    MarketplaceSubmissionsConnector.getInstance().objectMapper
  }

  @Test
  fun `test creating state on close`() {
    val eduTask = course.allTasks[0]
    val solutionFiles = getSolutionFiles(project, eduTask)
    val firstSolutionFile = solutionFiles.first()
    val placeholder = AnswerPlaceholder(2, "placeholder text")
    placeholder.init(eduTask.taskFiles["src/Task.kt"]!!, false)
    firstSolutionFile.placeholders = listOf(placeholder)
    val objectMapper = MarketplaceSubmissionsConnector.getInstance().objectMapper
    val solutionText = objectMapper.writeValueAsString(solutionFiles).trimIndent()
    val stateOnClose = MarketplaceStateOnClosePost(eduTask.id, solutionText)
    // Directly serialize the MarketplaceStateOnClosePost object using the objectMapper
    val actual = objectMapper.writeValueAsString(stateOnClose)
    val expected =
      """{"task_id":1,"solution":"[{\"name\":\"src/Task.kt\",\"placeholders\":[{\"offset\":2,\"length\":16,\"possible_answer\":\"\",\"placeholder_text\":\"placeholder text\"}],\"is_visible\":true,\"text\":\"solution file text\"},{\"name\":\"src/Test.kt\",\"placeholders\":null,\"is_visible\":false,\"text\":\"test file text\"}]","format_version":$JSON_FORMAT_VERSION}"""

    assertEquals(expected, actual)
  }

  @Test
  fun `test state on close deserialization`() {
    val submissionId = 21556587
    val submissionTime = Date()
    val courseVersion = 3
    val taskId = 5
    val solutionKey = "https://example"
    val jsonContent = """
        {
          "id": $submissionId,
          "time": ${submissionTime.time},
          "task_id": $taskId,
          "solution_aws_key": "$solutionKey",
          "format_version": $JSON_FORMAT_VERSION,
          "update_version": $courseVersion
        }
      """
    val stateOnClose = objectMapper.readValue<MarketplaceStateOnClose>(jsonContent)
    assertEquals(submissionId, stateOnClose.id)
    assertEquals(taskId, stateOnClose.taskId)
    assertEquals(submissionTime, stateOnClose.time)
    assertEquals(JSON_FORMAT_VERSION, stateOnClose.formatVersion)
    assertEquals(courseVersion, stateOnClose.courseVersion)
    assertEquals(solutionKey, stateOnClose.solutionKey)
  }
}