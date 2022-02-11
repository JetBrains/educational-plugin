package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.STUDENT_MAPPER
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

  fun `test marketplace edu task submission serialization`() {
    val eduTask = course.allTasks[0]
    val solutionFiles = getSolutionFiles(project, eduTask)
    val firstSolutionFile = solutionFiles.first()
    val placeholder = AnswerPlaceholder(2, "placeholder text")
    placeholder.initAnswerPlaceholder(eduTask.taskFiles["src/Task.kt"], false)
    firstSolutionFile.placeholders = listOf(placeholder)
    val submission = MarketplaceSubmission(eduTask.id, eduTask.status, solutionFiles, course.marketplaceCourseVersion)
    val submissionTime = submission.time ?: error("Time must be specified")
    val submissionId = submission.id ?: error("Id must be specified")

    doTest(submission, """
      |id: $submissionId
      |time: ${submissionTime.time}
      |status: Unchecked
      |course_version: 3
      |task_id: 1
      |solution:
      |- name: src/Task.kt
      |  text: solution file text
      |  is_visible: true
      |  placeholders:
      |  - offset: 2
      |    length: 16
      |    placeholder_text: placeholder text
      |    initial_state:
      |      length: 16
      |      offset: 2
      |    initialized_from_dependency: false
      |    selected: false
      |    status: Unchecked
      |version: $JSON_FORMAT_VERSION
      |
    """.trimMargin())
  }

  fun `test marketplace submission deserialization`() {
    val submissionId = 21556587
    val submissionTime = Date()
    val courseVersion = 3
    val taskId = 5
    val yamlContent = """
      |id: $submissionId
      |time: ${submissionTime.time}
      |task_id: $taskId
      |status: Solved
      |solution:
      |- name: src/Task.kt
      |  text: solution file text
      |  is_visible: true
      |  placeholders:
      |  - offset: 2
      |    length: 17
      |    placeholder_text: placeholder text
      |    initial_state:
      |      length: 16
      |      offset: 2
      |    initialized_from_dependency: false
      |    selected: false
      |    status: Unchecked
      |- name: src/Test.kt
      |  text: test file text
      |  is_visible: false
      |version: $JSON_FORMAT_VERSION
      |course_version: $courseVersion
      |
    """.trimMargin()
    val treeNode = STUDENT_MAPPER.readTree(yamlContent)
    val submission = STUDENT_MAPPER.treeToValue(treeNode, MarketplaceSubmission::class.java)
    checkNotNull(submission)
    assertEquals(submissionId, submission.id)
    assertEquals(taskId, submission.taskId)
    assertEquals(submissionTime, submission.time)
    assertEquals(CheckStatus.Solved.name, submission.status)
    assertEquals(JSON_FORMAT_VERSION, submission.formatVersion)
    assertEquals(courseVersion, submission.courseVersion)
    val firstSolutionFile = SolutionFile("src/Task.kt", "solution file text", true)
    val placeholder = AnswerPlaceholder(2, "placeholder text")
    firstSolutionFile.placeholders = listOf(placeholder)
    val expectedSolutionFilesList = listOf(firstSolutionFile, SolutionFile("src/Test.kt", "test file text", false))
    checkSolutionFiles(expectedSolutionFilesList, submission.solutionFiles)
  }

  private fun checkSolutionFiles(expectedList: List<SolutionFile>, actualList: List<SolutionFile>?) {
    checkNotNull(actualList)
    assertEquals(expectedList.size, actualList.size)
    for (n in expectedList.indices) {
      val expected = expectedList[n]
      val actual = actualList[n]
      assertEquals(expected.name, actual.name)
      assertEquals(expected.isVisible, actual.isVisible)
      assertEquals(expected.text, actual.text)
      if (expected.placeholders.isNullOrEmpty()) continue
      checkNotNull(actual.placeholders)
      assertEquals(expected.placeholders!!.size, actual.placeholders!!.size)
      assertEquals(expected.placeholders!!.first().placeholderText, actual.placeholders!!.first().placeholderText)
    }
  }

  private fun doTest(submission: MarketplaceSubmission, expected: String) {
    val actual = STUDENT_MAPPER.writeValueAsString(submission)
    assertEquals(expected, actual)
  }
}