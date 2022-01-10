package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.checker.CheckUtils.CONGRATULATIONS
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillSubmissionProvider.createEduSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillSubmissionProvider.createRemoteEduTaskSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class HyperskillCreateSubmissionTest : EduTestCase() {
  fun `test creating submission for solved edu task`() {
    val course = createHyperskillCourse()

    val eduTask = course.allTasks[0].apply { status = CheckStatus.Solved }
    val attempt = Attempt().apply { id = 123 }
    val solutionFiles = getSolutionFiles(project, eduTask).onError { error(it) }
    val feedback = CONGRATULATIONS
    val submission = createEduSubmission(eduTask, attempt, solutionFiles, feedback)

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
      |step: -1
      |
    """.trimMargin())
  }

  fun `test creating submission for failed edu task`() {
    val course = createHyperskillCourse()

    val eduTask = course.allTasks[0].apply { status = CheckStatus.Failed }
    val attempt = Attempt().apply { id = 1234 }
    val solutionFiles = getSolutionFiles(project, eduTask).onError { error(it) }
    val feedback = "failed"
    val submission = createEduSubmission(eduTask, attempt, solutionFiles, feedback)

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
      |step: -1
      |
    """.trimMargin())
  }

  fun `test creating submission for remote edu task`() {
    val course = createHyperskillCourse()

    val remoteEduTask = course.allTasks[1] as RemoteEduTask
    val checkProfile = remoteEduTask.checkProfile
    val attempt = Attempt().apply { id = 12345 }
    val solutionFiles = getSolutionFiles(project, remoteEduTask).onError { error(it) }
    val submission = createRemoteEduTaskSubmission(checkProfile, attempt, solutionFiles)

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
      |step: -1
      |
    """.trimMargin()
    )
  }

  private fun createHyperskillCourse() = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
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
      }
    }
  } as HyperskillCourse

  private fun doTest(submission: Submission, expected: String) {
    val actual = YamlFormatSynchronizer.STUDENT_MAPPER.writeValueAsString(submission)
    assertEquals(expected, actual)
  }
}