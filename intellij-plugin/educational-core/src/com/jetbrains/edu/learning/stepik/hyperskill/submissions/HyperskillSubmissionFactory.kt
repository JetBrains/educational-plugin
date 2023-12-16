package com.jetbrains.edu.learning.stepik.hyperskill.submissions

import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.TableTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.attempts.DataTaskAttempt
import com.jetbrains.edu.learning.stepik.api.Feedback
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.edu.learning.submissions.SolutionFile

object HyperskillSubmissionFactory {
  fun createCodeTaskSubmission(attempt: Attempt, answer: String, language: String): StepikBasedSubmission {
    val reply = CodeTaskReply()
    reply.code = answer
    reply.language = language
    return StepikBasedSubmission(attempt, reply)
  }

  fun createEduTaskSubmission(task: Task, attempt: Attempt, files: List<SolutionFile>, feedback: String): StepikBasedSubmission {
    val reply = EduTaskReply()
    reply.feedback = Feedback(feedback)
    reply.score = if (task.status == CheckStatus.Solved) "1" else "0"
    reply.solution = files
    return StepikBasedSubmission(attempt, reply)
  }

  fun createRemoteEduTaskSubmission(task: RemoteEduTask, attempt: Attempt, files: List<SolutionFile>): StepikBasedSubmission {
    val reply = EduTaskReply()
    reply.checkProfile = task.checkProfile
    reply.solution = files
    return StepikBasedSubmission(attempt, reply)
  }

  fun createChoiceTaskSubmission(task: ChoiceTask, attempt: Attempt): StepikBasedSubmission {
    val answerArray = createChoiceTaskAnswerArray(task, attempt)
    val reply = ChoiceTaskReply()
    reply.choices = answerArray
    return StepikBasedSubmission(attempt, reply)
  }

  private fun createChoiceTaskAnswerArray(task: ChoiceTask, attempt: Attempt): BooleanArray {
    val options = attempt.dataset?.options
    val answer = BooleanArray(task.choiceOptions.size)
    if (!options.isNullOrEmpty()) {
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
    val reply = TextTaskReply()
    reply.text = answer
    return StepikBasedSubmission(attempt, reply)
  }

  fun createNumberTaskSubmission(attempt: Attempt, answer: String): StepikBasedSubmission {
    val reply = NumberTaskReply()
    reply.number = answer
    return StepikBasedSubmission(attempt, reply)
  }

  fun createDataTaskSubmission(attempt: DataTaskAttempt, answer: String): StepikBasedSubmission {
    val reply = DataTaskReply()
    reply.file = answer
    return StepikBasedSubmission(attempt, reply)
  }

  fun createSortingBasedTaskSubmission(attempt: Attempt, ordering: IntArray): StepikBasedSubmission {
    val reply = SortingBasedTaskReply()
    reply.ordering = ordering
    return StepikBasedSubmission(attempt, reply)
  }

  fun createTableTaskSubmission(attempt: Attempt, task: TableTask): StepikBasedSubmission {
    val reply = TableTaskReply()

    reply.choices = Array(task.rows.size) { rowIndex ->
      Row().apply {
        nameRow = task.rows[rowIndex]
        columns = Array(task.columns.size) { columnIndex ->
          Column().apply {
            name = task.columns[columnIndex]
            answer = task.selected[rowIndex][columnIndex]
          }
        }
      }
    }
    return StepikBasedSubmission(attempt, reply)
  }
}