package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.api.Dataset
import com.jetbrains.edu.learning.stepik.submissions.StepikBaseSubmissionFactory.createChoiceTaskSubmission
import com.jetbrains.edu.learning.stepik.submissions.StepikBaseSubmissionFactory.createCodeTaskSubmission
import com.jetbrains.edu.learning.stepik.submissions.StepikBaseSubmissionFactory.createStepikSubmission
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.SubmissionData
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class StepikCreateSubmissionTest : EduTestCase() {
  private val stepikCourse: EduCourse by lazy {
    courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::EduCourse) {
      section("Section") {
        lesson("Lesson") {
          eduTask("Edu problem", stepId = 1) {
            taskFile("src/Task.kt")
            taskFile("src/Test.kt", visible = false)
          }
          choiceTask("Choice task", stepId = 2, isMultipleChoice = true,
                     choiceOptions = mapOf("Correct" to ChoiceOptionStatus.CORRECT,
                                           "Incorrect" to ChoiceOptionStatus.INCORRECT,
                                           "Unknown" to ChoiceOptionStatus.UNKNOWN)) {
            taskFile("Task.txt", "")
          }
        }
      }
    } as EduCourse
  }

  fun `test creating submission for code task`() {
    val attempt = Attempt().apply { id = 123 }
    val answer = "answer"
    val language = "language"
    val submission = createCodeTaskSubmission(attempt, answer, language).toSubmissionData()

    doTest(submission, """
      |submission:
      |  attempt: 123
      |  reply:
      |    language: $language
      |    code: $answer
      |    version: $JSON_FORMAT_VERSION
      |
    """.trimMargin())
  }

  fun `test creating submission for edu task`() {
    val eduTask = stepikCourse.allTasks[0]
    val attempt = Attempt().apply { id = 123 }
    val solutionFiles = getSolutionFiles(project, eduTask)
    val submission = createStepikSubmission(eduTask, attempt, solutionFiles).toSubmissionData()

    doTest(submission, """
      |submission:
      |  attempt: 123
      |  reply:
      |    score: 0
      |    solution:
      |    - name: src/Task.kt
      |      is_visible: true
      |    - name: src/Test.kt
      |      is_visible: false
      |    edu_task: '{"task":{"name":"Edu problem","stepic_id":1,"status":"Unchecked","files":{"src/Task.kt":{"name":"src/Task.kt","placeholders":[],"is_visible":true,"text":""},"src/Test.kt":{"name":"src/Test.kt","placeholders":[],"is_visible":false,"text":""}},"task_type":"edu"}}'
      |    version: $JSON_FORMAT_VERSION
      |
    """.trimMargin())
  }

  fun `test creating submission for choice task`() {
    val task = stepikCourse.allTasks.find { it.id == 2 } as ChoiceTask
    task.selectedVariants = mutableListOf(1)
    val dataset = Dataset().apply {
      options = task.choiceOptions.map { it.text }
    }
    val attempt = Attempt().apply {
      id = 123
      this.dataset = dataset
    }

    val submission = createChoiceTaskSubmission(task, attempt).toSubmissionData()
    doTest(submission, """
      |submission:
      |  attempt: 123
      |  reply:
      |    choices:
      |    - false
      |    - true
      |    - false
      |    version: $JSON_FORMAT_VERSION
      |
    """.trimMargin())
  }

  private fun doTest(submissionData: SubmissionData, expected: String) {
    val actual = YamlFormatSynchronizer.STUDENT_MAPPER.writeValueAsString(submissionData)
    assertEquals(expected, actual)
  }

  private fun Submission.toSubmissionData(): SubmissionData = SubmissionData(this)
}