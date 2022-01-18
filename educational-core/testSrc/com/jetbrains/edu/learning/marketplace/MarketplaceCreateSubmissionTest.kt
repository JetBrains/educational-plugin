package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.stepik.submissions.StepikBasedSubmissionFactory.createMarketplaceSubmissionData
import com.jetbrains.edu.learning.submissions.SubmissionData
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class MarketplaceCreateSubmissionTest : EduTestCase() {
  private val course: EduCourse by lazy {
    courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::EduCourse) {
      section("Section") {
        lesson("Lesson") {
          eduTask("Edu problem", stepId = 1) {
            taskFile("src/Task.kt")
            taskFile("src/Test.kt", visible = false)
          }
        }
      }
    } as EduCourse
  }

  fun `test creating submission for edu task`() {
    val eduTask = course.allTasks[0]
    val solutionFiles = getSolutionFiles(project, eduTask)
    val submission = createMarketplaceSubmissionData(eduTask, solutionFiles)
    val submissionTime = submission.submission.time ?: error("Time must be specified")
    val submissionId = submission.submission.id ?: error("Id must be specified")

    doTest(submission, """
      |submission:
      |  attempt: 0
      |  reply:
      |    score: 0
      |    solution:
      |    - name: src/Task.kt
      |      is_visible: true
      |    - name: src/Test.kt
      |      is_visible: false
      |    edu_task: '{"task":{"name":"Edu problem","stepic_id":1,"status":"Unchecked","files":{"src/Task.kt":{"name":"src/Task.kt","placeholders":[],"is_visible":true,"text":""},"src/Test.kt":{"name":"src/Test.kt","placeholders":[],"is_visible":false,"text":""}},"task_type":"edu"}}'
      |    version: $JSON_FORMAT_VERSION
      |  id: $submissionId
      |  time: ${submissionTime.time}
      |
    """.trimMargin())
  }

  private fun doTest(submissionData: SubmissionData, expected: String) {
    val actual = YamlFormatSynchronizer.STUDENT_MAPPER.writeValueAsString(submissionData)
    assertEquals(expected, actual)
  }
}