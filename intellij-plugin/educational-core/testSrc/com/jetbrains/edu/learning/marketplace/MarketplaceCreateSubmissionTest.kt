package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.WRONG
import com.jetbrains.edu.learning.courseFormat.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import com.jetbrains.edu.learning.yaml.YamlMapper.studentMapper
import org.junit.Test
import java.util.*

class MarketplaceCreateSubmissionTest : EduTestCase() {
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

  @Test
  fun `test creating submission for solved edu task`() = createSubmission(CORRECT, CheckStatus.Solved)
  @Test
  fun `test creating submission for failed edu task`() = createSubmission(WRONG, CheckStatus.Failed)
  @Test
  fun `test correct submission deserialization`() = deserializeSubmission(CORRECT, CheckStatus.Solved)
  @Test
  fun `test wrong submission deserialization`() = deserializeSubmission(WRONG, CheckStatus.Failed)

  private fun createSubmission(submissionStatus: String, checkStatus: CheckStatus) {
    val eduTask = course.allTasks[0]
    eduTask.status = checkStatus
    val solutionFiles = getSolutionFiles(project, eduTask)
    val firstSolutionFile = solutionFiles.first()
    val placeholder = AnswerPlaceholder(2, "placeholder text")
    placeholder.init(eduTask.taskFiles["src/Task.kt"]!!, false)
    firstSolutionFile.placeholders = listOf(placeholder)
    val objectMapper = MarketplaceSubmissionsConnector.getInstance().objectMapper
    val solutionText = objectMapper.writeValueAsString(solutionFiles).trimIndent()
    val submission = MarketplaceSubmission(eduTask.id, eduTask.status, solutionText, solutionFiles, course.marketplaceCourseVersion)

    doTest(submission, """
      |status: $submissionStatus
      |update_version: 3
      |task_id: 1
      |format_version: $JSON_FORMAT_VERSION
      |solution: "[{\"name\":\"src/Task.kt\",\"placeholders\":[{\"offset\":2,\"length\":16,\"\
  possible_answer\":\"\",\"placeholder_text\":\"placeholder text\"}],\"is_visible\"\
  :true,\"text\":\"solution file text\"},{\"name\":\"src/Test.kt\",\"placeholders\"\
  :null,\"is_visible\":false,\"text\":\"test file text\"}]"
      |
    """.trimMargin())
  }

  private fun deserializeSubmission(submissionStatus: String, checkStatus: CheckStatus) {
    val submissionId = 21556587
    val submissionTime = Date()
    val courseVersion = 3
    val taskId = 5
    val solutionKey = "https://example"
    val yamlContent = """
      |id: $submissionId
      |time: ${submissionTime.time}
      |task_id: $taskId
      |status: $submissionStatus
      |solution_aws_key: $solutionKey
      |format_version: $JSON_FORMAT_VERSION
      |update_version: $courseVersion
      |
    """.trimMargin()
    val studentMapper = studentMapper()
    val treeNode = studentMapper.readTree(yamlContent)
    val submission = studentMapper.treeToValue(treeNode, MarketplaceSubmission::class.java)
    checkNotNull(submission)
    assertEquals(submissionId, submission.id)
    assertEquals(taskId, submission.taskId)
    assertEquals(submissionTime, submission.time)
    assertEquals(submissionStatus, submission.status)
    assertEquals(JSON_FORMAT_VERSION, submission.formatVersion)
    assertEquals(courseVersion, submission.courseVersion)
    assertEquals(solutionKey, submission.solutionKey)
    assertEquals(checkStatus.rawStatus, submission.status)
  }

  private fun doTest(submission: MarketplaceSubmission, expected: String) {
    val actual = studentMapper().writeValueAsString(submission)
    assertEquals(expected, actual)
  }
}