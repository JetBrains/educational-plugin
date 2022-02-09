package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.checker.CheckUtils.CONGRATULATIONS
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.stepik.StepikBasedCreateSubmissionTest
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.api.Dataset
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.submissions.StepikBasedSubmissionFactory.createChoiceTaskSubmission
import com.jetbrains.edu.learning.stepik.submissions.StepikBasedSubmissionFactory.createCodeTaskSubmission
import com.jetbrains.edu.learning.stepik.submissions.StepikBasedSubmissionFactory.createDataTaskSubmission
import com.jetbrains.edu.learning.stepik.submissions.StepikBasedSubmissionFactory.createEduTaskSubmission
import com.jetbrains.edu.learning.stepik.submissions.StepikBasedSubmissionFactory.createNumberTaskSubmission
import com.jetbrains.edu.learning.stepik.submissions.StepikBasedSubmissionFactory.createRemoteEduTaskSubmission
import com.jetbrains.edu.learning.stepik.submissions.StepikBasedSubmissionFactory.createStringTaskSubmission
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.util.*

class HyperskillCreateSubmissionTest : StepikBasedCreateSubmissionTest() {
  private val hyperskillCourse: HyperskillCourse by lazy {
    courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      section("Topics") {
        lesson("Topic name") {
          eduTask("Edu problem", stepId = 1) {
            taskFile("src/Task.kt")
            taskFile("src/Test.kt", visible = false)
          }
          remoteEduTask("Remote Edu problem", stepId = 2, checkProfile = "hyperskill_go") {
            taskFile("src/Task.kt")
            taskFile("src/Test.kt", visible = false)
          }
          choiceTask("Choice task", stepId = 3, isMultipleChoice = true,
                     choiceOptions = mapOf("Correct" to ChoiceOptionStatus.CORRECT,
                                           "Incorrect" to ChoiceOptionStatus.INCORRECT,
                                           "Unknown" to ChoiceOptionStatus.UNKNOWN)) {
            taskFile("Task.txt", "")
          }
        }
      }
    } as HyperskillCourse
  }

  fun `test creating submission for solved edu task`() {
    val eduTask = hyperskillCourse.allTasks[0].apply { status = CheckStatus.Solved }
    val attempt = Attempt().apply { id = 123 }
    val solutionFiles = getSolutionFiles(project, eduTask)
    val feedback = CONGRATULATIONS
    val submission = createEduTaskSubmission(eduTask, attempt, solutionFiles, feedback)

    doTest(submission, """
      |attempt: 123
      |reply:
      |  score: 1
      |  solution:
      |  - name: src/Task.kt
      |    is_visible: true
      |  - name: src/Test.kt
      |    is_visible: false
      |  version: $JSON_FORMAT_VERSION
      |  feedback:
      |    message: $feedback
      |
    """.trimMargin())
  }

  fun `test creating submission for failed edu task`() {
    val eduTask = hyperskillCourse.allTasks[0].apply { status = CheckStatus.Failed }
    val attempt = Attempt().apply { id = 1234 }
    val solutionFiles = getSolutionFiles(project, eduTask)
    val feedback = "failed"
    val submission = createEduTaskSubmission(eduTask, attempt, solutionFiles, feedback)

    doTest(submission, """
      |attempt: 1234
      |reply:
      |  score: 0
      |  solution:
      |  - name: src/Task.kt
      |    is_visible: true
      |  - name: src/Test.kt
      |    is_visible: false
      |  version: $JSON_FORMAT_VERSION
      |  feedback:
      |    message: $feedback
      |
    """.trimMargin())
  }

  fun `test creating submission for remote edu task`() {
    val remoteEduTask = hyperskillCourse.allTasks[1] as RemoteEduTask
    val checkProfile = remoteEduTask.checkProfile
    val attempt = Attempt().apply { id = 12345 }
    val solutionFiles = getSolutionFiles(project, remoteEduTask)
    val submission = createRemoteEduTaskSubmission(remoteEduTask, attempt, solutionFiles)

    doTest(submission, """
      |attempt: 12345
      |reply:
      |  solution:
      |  - name: src/Task.kt
      |    is_visible: true
      |  - name: src/Test.kt
      |    is_visible: false
      |  version: $JSON_FORMAT_VERSION
      |  check_profile: $checkProfile
      |
    """.trimMargin()
    )
  }

  fun `test creating submission for code task`() {
    val attempt = Attempt().apply { id = 123 }
    val answer = "answer"
    val language = "language"
    val submission = createCodeTaskSubmission(attempt, answer, language)

    doTest(submission, """
      |attempt: 123
      |reply:
      |  language: $language
      |  code: $answer
      |  version: $JSON_FORMAT_VERSION
      |
    """.trimMargin())
  }

  fun `test creating submission for choice task`() {
    val task = hyperskillCourse.allTasks.find { it.id == 3 } as ChoiceTask
    task.selectedVariants = mutableListOf(0)
    val dataset = Dataset().apply {
      options = task.choiceOptions.map { it.text }
    }
    val attempt = Attempt().apply {
      id = 123
      this.dataset = dataset
    }

    val submission = createChoiceTaskSubmission(task, attempt)
    doTest(submission, """
      |attempt: 123
      |reply:
      |  choices:
      |  - true
      |  - false
      |  - false
      |  version: $JSON_FORMAT_VERSION
      |
    """.trimMargin())
  }

  fun `test creating submission for string task`() {
    val attempt = Attempt().apply { id = 123 }
    val answer = "answer"

    val submission = createStringTaskSubmission(attempt, answer)
    doTest(submission, """
      |attempt: 123
      |reply:
      |  text: $answer
      |  version: $JSON_FORMAT_VERSION
      |
    """.trimMargin())
  }

  fun `test creating submission for number task`() {
    val attempt = Attempt().apply { id = 123 }
    val answer = 123.toString()

    val submission = createNumberTaskSubmission(attempt, answer)
    doTest(submission, """
      |attempt: 123
      |reply:
      |  number: $answer
      |  version: $JSON_FORMAT_VERSION
      |
    """.trimMargin())
  }

  fun `test creating submission for data task`() {
    val dataTaskAttempt = Attempt(123, Date(), 300).toDataTaskAttempt()
    val answer = "answer"

    val submission = createDataTaskSubmission(dataTaskAttempt, answer)
    doTest(submission, """
      |attempt: 123
      |reply:
      |  file: $answer
      |  version: $JSON_FORMAT_VERSION
      |
    """.trimMargin())
  }

  private fun doTest(submission: StepikBasedSubmission, expected: String) {
    val actual = YamlFormatSynchronizer.STUDENT_MAPPER.writeValueAsString(submission)
    assertEquals(expected, actual)
  }
}