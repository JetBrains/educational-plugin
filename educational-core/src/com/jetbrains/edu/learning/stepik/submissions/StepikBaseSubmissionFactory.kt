package com.jetbrains.edu.learning.stepik.submissions

import com.fasterxml.jackson.databind.module.SimpleModule
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTaskAttempt
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.SubmissionData
import java.util.*

object StepikBaseSubmissionFactory {
  @JvmStatic
  fun createCodeTaskSubmission(attempt: Attempt, answer: String, language: String): Submission {
    val reply = Reply()
    reply.code = answer
    reply.language = language
    return Submission(attempt, reply)
  }

  fun createEduTaskSubmission(task: Task, attempt: Attempt, files: List<SolutionFile>, feedback: String): Submission {
    val reply = Reply()
    reply.feedback = Feedback(feedback)
    reply.score = if (task.status == CheckStatus.Solved) "1" else "0"
    reply.solution = files
    return Submission(attempt, reply)
  }

  // to be used for marketplace submissions creation
  // temporary for now and will be removed
  fun createMarketplaceSubmissionData(
    task: Task,
    files: List<SolutionFile> = emptyList(),
    passed: Boolean = task.status == CheckStatus.Solved
  ): SubmissionData {
    val objectMapper = StepikConnector.createMapper(SimpleModule())
    val serializedTask = objectMapper.writeValueAsString(TaskData(task))

    val reply = Reply()
    reply.eduTask = serializedTask
    reply.score = if (passed) "1" else "0"
    reply.solution = files

    val submission = Submission()
    submission.reply = reply
    submission.time = Date()

    // don't forget to remove SubmissionData() constructor
    val submissionData = SubmissionData()
    submissionData.submission = submission
    submissionData.submission.id = submissionData.hashCode()

    return submissionData
  }

  @JvmStatic
  fun createStepikSubmission(task: Task, attempt: Attempt, files: List<SolutionFile> = emptyList()): Submission {
    val objectMapper = StepikConnector.createMapper(SimpleModule())
    val serializedTask = objectMapper.writeValueAsString(TaskData(task))

    val reply = Reply()
    reply.eduTask = serializedTask
    reply.score = if (task.status == CheckStatus.Solved) "1" else "0"
    reply.solution = files
    return Submission(attempt, reply)
  }

  fun createRemoteEduTaskSubmission(task: RemoteEduTask, attempt: Attempt, files: List<SolutionFile>): Submission {
    val reply = Reply()
    reply.checkProfile = task.checkProfile
    reply.solution = files
    return Submission(attempt, reply)
  }

  @JvmStatic
  fun createChoiceTaskSubmission(task: ChoiceTask, attempt: Attempt): Submission {
    val answerArray = createChoiceTaskAnswerArray(task, attempt)
    val reply = Reply()
    reply.choices = answerArray
    return Submission(attempt, reply)
  }

  private fun createChoiceTaskAnswerArray(task: ChoiceTask, attempt: Attempt): BooleanArray {
    val options = attempt.dataset?.options
    val answer = BooleanArray(task.choiceOptions.size)
    if (options != null && options.isNotEmpty()) {
      // Every attempt of choiceTask can return options in different order
      task.selectedVariants
        .map { selectedIndex -> task.choiceOptions[selectedIndex] }
        .map(ChoiceOption::text)
        .map { selectedText -> options.indexOf(selectedText) }
        .forEach { index -> answer[index] = true }
    }

    return answer
  }

  fun createStringTaskSubmission(attempt: Attempt, answer: String): Submission {
    val reply = Reply()
    reply.text = answer
    return Submission(attempt, reply)
  }

  fun createDataTaskSubmission(attempt: DataTaskAttempt, answer: String): Submission {
    val reply = Reply()
    reply.file = answer
    return Submission(attempt, reply)
  }
}