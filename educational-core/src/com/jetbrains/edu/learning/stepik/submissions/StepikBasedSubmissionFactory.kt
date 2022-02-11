package com.jetbrains.edu.learning.stepik.submissions

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTaskAttempt
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.submissions.SolutionFile

object StepikBasedSubmissionFactory {
  @JvmStatic
  fun createCodeTaskSubmission(attempt: Attempt, answer: String, language: String): StepikBasedSubmission {
    val reply = Reply()
    reply.code = answer
    reply.language = language
    return StepikBasedSubmission(attempt, reply)
  }

  fun createEduTaskSubmission(task: Task, attempt: Attempt, files: List<SolutionFile>, feedback: String): StepikBasedSubmission {
    val reply = Reply()
    reply.feedback = Feedback(feedback)
    reply.score = if (task.status == CheckStatus.Solved) "1" else "0"
    reply.solution = files
    return StepikBasedSubmission(attempt, reply)
  }

  @JvmStatic
  fun createStepikSubmission(task: Task, attempt: Attempt, files: List<SolutionFile> = emptyList()): StepikBasedSubmission {
    val objectMapper = StepikConnector.getInstance().objectMapper
    val serializedTask = objectMapper.writeValueAsString(TaskData(task))

    val reply = Reply()
    reply.eduTask = serializedTask
    reply.score = if (task.status == CheckStatus.Solved) "1" else "0"
    reply.solution = files
    return StepikBasedSubmission(attempt, reply)
  }

  fun createRemoteEduTaskSubmission(task: RemoteEduTask, attempt: Attempt, files: List<SolutionFile>): StepikBasedSubmission {
    val reply = Reply()
    reply.checkProfile = task.checkProfile
    reply.solution = files
    return StepikBasedSubmission(attempt, reply)
  }

  @JvmStatic
  fun createChoiceTaskSubmission(task: ChoiceTask, attempt: Attempt): StepikBasedSubmission {
    val answerArray = createChoiceTaskAnswerArray(task, attempt)
    val reply = Reply()
    reply.choices = answerArray
    return StepikBasedSubmission(attempt, reply)
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

  fun createStringTaskSubmission(attempt: Attempt, answer: String): StepikBasedSubmission {
    val reply = Reply()
    reply.text = answer
    return StepikBasedSubmission(attempt, reply)
  }

  fun createNumberTaskSubmission(attempt: Attempt, answer: String): StepikBasedSubmission {
    val reply = Reply()
    reply.number = answer
    return StepikBasedSubmission(attempt, reply)
  }

  fun createDataTaskSubmission(attempt: DataTaskAttempt, answer: String): StepikBasedSubmission {
    val reply = Reply()
    reply.file = answer
    return StepikBasedSubmission(attempt, reply)
  }
}