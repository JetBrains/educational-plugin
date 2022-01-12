package com.jetbrains.edu.learning.stepik.checker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getText
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
import com.jetbrains.edu.learning.stepik.api.SolutionFile
import com.jetbrains.edu.learning.stepik.api.StepikBaseConnector.Companion.getStepikBaseConnector
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLanguages
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.submissions.StepikBaseSubmissionFactory
import com.jetbrains.edu.learning.submissions.Submission

object StepikBaseSubmitConnector {
  fun submitCodeTask(project: Project, task: CodeTask): Result<Submission, String> {
    val connector = task.getStepikBaseConnector()
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
      return Err(EduCoreBundle.message("error.failed.to.post.solution", connector.platformName))
    }
    val submission = StepikBaseSubmissionFactory.createCodeTaskSubmission(attempt, codeTaskText, defaultLanguage)
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

  fun submitChoiceTask(task: ChoiceTask): Result<Submission, String> {
    val connector = task.getStepikBaseConnector()
    val attempt = connector.getActiveAttemptOrPostNew(task).onError {
      return Err(it)
    }

    val submission = StepikBaseSubmissionFactory.createChoiceTaskSubmission(task, attempt)
    return connector.postSubmission(submission)
  }

  // will be supported for Stepik in EDU-4744
  fun submitDataTask(task: DataTask, answer: String): Result<Submission, String> {
    val attempt = task.attempt ?: return Err("Impossible to submit data task without active attempt")
    val connector = HyperskillConnector.getInstance()
    val submission = StepikBaseSubmissionFactory.createDataTaskSubmission(attempt, answer)
    return connector.postSubmission(submission)
  }

  // will be supported for Stepik in EDU-4763, EDU-4780
  fun submitAnswerTask(project: Project, task: AnswerTask): Result<Submission, String> {
    val connector = HyperskillConnector.getInstance()
    val attempt = connector.postAttempt(task).onError {
      return Err(it)
    }

    val submission = when (task) {
      is StringTask -> StepikBaseSubmissionFactory.createStringTaskSubmission(attempt, task.getInputAnswer(project))
      is NumberTask -> StepikBaseSubmissionFactory.createNumberTaskSubmission(attempt, task.getInputAnswer(project))
    }
    return connector.postSubmission(submission)
  }

  fun submitRemoteEduTask(task: RemoteEduTask, files: List<SolutionFile>): Result<Submission, String> {
    val connector = HyperskillConnector.getInstance()
    val attempt = connector.postAttempt(task).onError {
      return Err(it)
    }

    val taskSubmission = StepikBaseSubmissionFactory.createRemoteEduTaskSubmission(task, attempt, files)
    return connector.postSubmission(taskSubmission)
  }

  @JvmStatic
  private val LOG: Logger = logger<StepikBaseSubmitConnector>()
}