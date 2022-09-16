package com.jetbrains.edu.learning.stepik.checker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getText
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.NumberTask
import com.jetbrains.edu.learning.courseFormat.tasks.StringTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.stepik.StepikLanguage
import com.jetbrains.edu.learning.stepik.api.StepikBasedConnector.Companion.getStepikBasedConnector
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLanguages
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.submissions.StepikBasedSubmissionFactory
import com.jetbrains.edu.learning.submissions.SolutionFile

object StepikBasedSubmitConnector {
  fun submitCodeTask(project: Project, task: CodeTask): Result<StepikBasedSubmission, String> {
    val connector = task.getStepikBasedConnector()
    val attempt = connector.postAttempt(task).onError {
      return Err(it)
    }

    val defaultLanguage = getLanguage(task).onError {
      return Err(it)
    }

    val configurator = task.course.configurator
    val codeTaskText = configurator?.getCodeTaskFile(project, task)?.getText(project)
    if (codeTaskText == null) {
      LOG.error("Unable to create submission: file with code is not found for the task ${task.name}")
      return Err(EduCoreBundle.message("error.failed.to.post.solution.to", connector.platformName))
    }
    val submission = StepikBasedSubmissionFactory.createCodeTaskSubmission(attempt, codeTaskText, defaultLanguage)
    return connector.postSubmission(submission)
  }

  // will be fixed or removed in EDU-4781
  private fun getLanguage(task: CodeTask): Result<String, String> {
    val course = task.course
    return when {
      course is HyperskillCourse -> getHyperskillLanguage(task)
      course.isStepikRemote -> getStepikLanguage(task)
      else -> error("Task ${task.name} doesn't belong to Hyperskill nor Stepik courses")
    }
  }

  private fun getHyperskillLanguage(task: CodeTask): Result<String, String> {
    val course = task.course
    val defaultLanguage = HyperskillLanguages.getLanguageName(course.languageID)
    if (defaultLanguage == null) {
      val languageDisplayName = course.languageDisplayName
      return Err("""Unknown language "$languageDisplayName". Check if support for "$languageDisplayName" is enabled.""")
    }
    return Ok(defaultLanguage)
  }

  private fun getStepikLanguage(task: CodeTask): Result<String, String> {
    val course = task.course

    val courseLanguageId = course.languageById?.id
    if (courseLanguageId != null) {
      val defaultLanguage = StepikLanguage.langOfId(courseLanguageId, course.languageVersion).langName
      if (defaultLanguage != null) {
        return Ok(defaultLanguage)
      }
    }

    val languageDisplayName = course.languageDisplayName
    return Err("""Unknown language "$languageDisplayName". Check if support for "$languageDisplayName" is enabled.""")
  }

  fun submitChoiceTask(task: ChoiceTask): Result<StepikBasedSubmission, String> {
    val connector = task.getStepikBasedConnector()
    val attempt = connector.getActiveAttemptOrPostNew(task).onError {
      return Err(it)
    }

    val submission = StepikBasedSubmissionFactory.createChoiceTaskSubmission(task, attempt)
    return connector.postSubmission(submission)
  }

  fun submitDataTask(task: DataTask, answer: String): Result<StepikBasedSubmission, String> {
    val attempt = task.attempt ?: return Err("Impossible to submit data task without active attempt")
    val connector = task.getStepikBasedConnector()
    val submission = StepikBasedSubmissionFactory.createDataTaskSubmission(attempt, answer)
    return connector.postSubmission(submission)
  }

  fun submitAnswerTask(project: Project, task: AnswerTask): Result<StepikBasedSubmission, String> {
    val connector = task.getStepikBasedConnector()
    val attempt = connector.postAttempt(task).onError {
      return Err(it)
    }

    val submission = when (task) {
      is StringTask -> StepikBasedSubmissionFactory.createStringTaskSubmission(attempt, task.getInputAnswer(project))
      is NumberTask -> StepikBasedSubmissionFactory.createNumberTaskSubmission(attempt, task.getInputAnswer(project))
    }
    return connector.postSubmission(submission)
  }

  fun submitRemoteEduTask(task: RemoteEduTask, files: List<SolutionFile>): Result<StepikBasedSubmission, String> {
    val connector = HyperskillConnector.getInstance()
    val attempt = connector.postAttempt(task).onError {
      return Err(it)
    }

    val taskSubmission = StepikBasedSubmissionFactory.createRemoteEduTaskSubmission(task, attempt, files)
    return connector.postSubmission(taskSubmission)
  }

  @JvmStatic
  private val LOG: Logger = logger<StepikBasedSubmitConnector>()
}