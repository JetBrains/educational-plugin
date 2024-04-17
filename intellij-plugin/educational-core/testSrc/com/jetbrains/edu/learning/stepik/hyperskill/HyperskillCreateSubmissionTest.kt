package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.checker.CheckUtils.CONGRATULATIONS
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.attempts.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.courseFormat.attempts.Dataset
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.RemoteEduTask
import com.jetbrains.edu.learning.courseFormat.tasks.TableTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory.createChoiceTaskSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory.createCodeTaskSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory.createDataTaskSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory.createEduTaskSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory.createNumberTaskSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory.createRemoteEduTaskSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory.createSortingBasedTaskSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory.createStringTaskSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory.createTableTaskSubmission
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import com.jetbrains.edu.learning.yaml.YamlMapper
import org.junit.Test
import java.util.*

class HyperskillCreateSubmissionTest : EduTestCase() {
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

  @Test
  fun `test creating submission for solved edu task`() {
    val eduTask = hyperskillCourse.allTasks[0].apply { status = CheckStatus.Solved }
    val attempt = Attempt().apply { id = 123 }
    val solutionFiles = getSolutionFiles(project, eduTask)
    val feedback = CONGRATULATIONS
    val submission = createEduTaskSubmission(eduTask, attempt, solutionFiles, feedback)

    doTest(submission, """
      |attempt: 123
      |reply:
      |  version: $JSON_FORMAT_VERSION
      |  feedback:
      |    message: $feedback
      |  score: 1
      |  solution:
      |  - name: src/Task.kt
      |    is_visible: true
      |  - name: src/Test.kt
      |    is_visible: false
      |
    """.trimMargin())
  }

  @Test
  fun `test creating submission for failed edu task`() {
    val eduTask = hyperskillCourse.allTasks[0].apply { status = CheckStatus.Failed }
    val attempt = Attempt().apply { id = 1234 }
    val solutionFiles = getSolutionFiles(project, eduTask)
    val feedback = "failed"
    val submission = createEduTaskSubmission(eduTask, attempt, solutionFiles, feedback)

    doTest(submission, """
      |attempt: 1234
      |reply:
      |  version: $JSON_FORMAT_VERSION
      |  feedback:
      |    message: $feedback
      |  score: 0
      |  solution:
      |  - name: src/Task.kt
      |    is_visible: true
      |  - name: src/Test.kt
      |    is_visible: false
      |
    """.trimMargin())
  }

  @Test
  fun `test creating submission for remote edu task`() {
    val remoteEduTask = hyperskillCourse.allTasks[1] as RemoteEduTask
    val checkProfile = remoteEduTask.checkProfile
    val attempt = Attempt().apply { id = 12345 }
    val solutionFiles = getSolutionFiles(project, remoteEduTask)
    val submission = createRemoteEduTaskSubmission(remoteEduTask, attempt, solutionFiles)

    doTest(submission, """
      |attempt: 12345
      |reply:
      |  version: $JSON_FORMAT_VERSION
      |  solution:
      |  - name: src/Task.kt
      |    is_visible: true
      |  - name: src/Test.kt
      |    is_visible: false
      |  check_profile: $checkProfile
      |
    """.trimMargin()
    )
  }

  @Test
  fun `test creating submission for code task`() {
    val attempt = Attempt().apply { id = 123 }
    val answer = "answer"
    val language = "language"
    val submission = createCodeTaskSubmission(attempt, answer, language)

    doTest(submission, """
      |attempt: 123
      |reply:
      |  version: $JSON_FORMAT_VERSION
      |  language: $language
      |  code: $answer
      |
    """.trimMargin())
  }

  @Test
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
      |  version: $JSON_FORMAT_VERSION
      |  choices:
      |  - true
      |  - false
      |  - false
      |
    """.trimMargin())
  }

  @Test
  fun `test creating submission for string task`() {
    val attempt = Attempt().apply { id = 123 }
    val answer = "answer"

    val submission = createStringTaskSubmission(attempt, answer)
    doTest(submission, """
      |attempt: 123
      |reply:
      |  version: $JSON_FORMAT_VERSION
      |  text: $answer
      |
    """.trimMargin())
  }

  @Test
  fun `test creating submission for number task`() {
    val attempt = Attempt().apply { id = 123 }
    val answer = 123.toString()

    val submission = createNumberTaskSubmission(attempt, answer)
    doTest(submission, """
      |attempt: 123
      |reply:
      |  version: $JSON_FORMAT_VERSION
      |  number: $answer
      |
    """.trimMargin())
  }

  @Test
  fun `test creating submission for data task`() {
    val dataTaskAttempt = Attempt(123, Date(), 300).toDataTaskAttempt()
    val answer = "answer"

    val submission = createDataTaskSubmission(dataTaskAttempt, answer)
    doTest(submission, """
      |attempt: 123
      |reply:
      |  version: $JSON_FORMAT_VERSION
      |  file: $answer
      |
    """.trimMargin())
  }

  @Test
  fun `test creating submission for sorting based task`() {
    val attempt = Attempt().apply { id = 123 }

    val ordering = intArrayOf(2, 0, 1)

    val submission = createSortingBasedTaskSubmission(attempt, ordering)
    doTest(submission, """
      |attempt: 123
      |reply:
      |  version: $JSON_FORMAT_VERSION
      |  ordering:
      |  - 2
      |  - 0
      |  - 1
      |
    """.trimMargin())
  }

  @Test
  fun `test creating submission for table task`() {
    val attempt = Attempt().apply { id = 123 }

    val course = courseWithFiles {
      lesson {
        tableTask(rows = listOf("A", "B"), columns = listOf("1", "2", "3"))
      }
    }

    val task = course.lessons.first().taskList.first() as TableTask

    task.choose(0, 1)
    task.choose(1, 2)

    val submission = createTableTaskSubmission(attempt, task)
    doTest(submission, """
      |attempt: 123
      |reply:
      |  version: $JSON_FORMAT_VERSION
      |  choices:
      |  - name_row: A
      |    columns:
      |    - name: 1
      |      answer: false
      |    - name: 2
      |      answer: true
      |    - name: 3
      |      answer: false
      |  - name_row: B
      |    columns:
      |    - name: 1
      |      answer: false
      |    - name: 2
      |      answer: false
      |    - name: 3
      |      answer: true
      |
    """.trimMargin())
  }

  private fun doTest(submission: StepikBasedSubmission, expected: String) {
    val actual = YamlMapper.STUDENT_MAPPER.writeValueAsString(submission)
    assertEquals(expected, actual)
  }
}