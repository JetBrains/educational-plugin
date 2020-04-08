package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikCheckerConnector
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.api.SolutionFile
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLoginListener
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.showErrorDetails
import java.util.concurrent.TimeUnit

object HyperskillCheckConnector {
  private val LOG = Logger.getInstance(HyperskillCheckConnector::class.java)
  private const val MAX_FILE_SIZE_FOR_PUBLISH = 5 * 1024 * 1024 // 5 Mb

  fun postSolution(task: Task, project: Project, result: CheckResult) {
    when (val attemptResponse = HyperskillConnector.getInstance().postAttempt(task.id)) {
      is Err -> showErrorDetails(project, attemptResponse.error)
      is Ok -> {
        val feedback = if (result.details == null) result.message else "${result.message}\n${result.details}"
        postEduSubmission(attemptResponse.value, project, task, feedback)
      }
    }
  }

  private fun postEduSubmission(attempt: Attempt, project: Project, task: Task, feedback: String) {
    val taskDir = task.getTaskDir(project)
    if (taskDir == null) {
      val error = EduCoreBundle.message("error.failed.to.find.dir", task.name)
      LOG.error(error)
      showErrorDetails(project, EduCoreBundle.message("error.unexpected", error))
      return
    }

    val files = ArrayList<SolutionFile>()
    for (taskFile in task.taskFiles.values) {
      val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
      if (virtualFile.length > MAX_FILE_SIZE_FOR_PUBLISH) {
        LOG.warn("File ${virtualFile.path} is too big (${virtualFile.length} bytes), will be ignored for submitting to the server")
        continue
      }

      runReadAction {
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return@runReadAction
        files.add(SolutionFile(taskFile.name, document.text))
      }
    }

    when (val submissionResponse = HyperskillConnector.getInstance().postSubmission(createEduSubmission(task, attempt, files, feedback))) {
      is Err -> showErrorDetails(project, submissionResponse.error)
    }
  }

  private fun createEduSubmission(task: Task, attempt: Attempt, files: List<SolutionFile>, feedback: String): Submission {
    val score = if (task.status == CheckStatus.Solved) "1" else "0"
    return Submission(score, attempt.id, files, null, feedback)
  }

  private fun Err<String>.toCheckResult(): CheckResult {
    return if (error == EduCoreBundle.message("error.forbidden")) {
      CheckResult(CheckStatus.Unchecked,
                  EduCoreBundle.message("error.forbidden.with.link"),
                  needEscape = false,
                  hyperlinkListener = HyperskillLoginListener
      )
    }
    else CheckResult(CheckStatus.Unchecked, error)
  }

  fun checkCodeTask(project: Project, task: Task): CheckResult {
    if (task.id == 0) {
      val link = task.feedbackLink.link ?: return CheckResult.FAILED_TO_CHECK
      val message = """Corrupted task (no id): please, click "Solve in IDE" on <a href="$link">${EduNames.JBA}</a> one more time"""
      return CheckResult(CheckStatus.Unchecked, message, needEscape = false)
    }
    val connector = HyperskillConnector.getInstance()
    val attempt = when (val attemptResponse = connector.postAttempt(task.id)) {
      is Err -> return attemptResponse.toCheckResult()
      is Ok -> attemptResponse.value
    }

    val course = task.lesson.course
    val editor = EduUtils.getSelectedEditor(project)
    if (editor == null) return CheckResult.FAILED_TO_CHECK

    val defaultLanguage = HyperskillLanguages.langOfId(course.languageID).langName
    if (defaultLanguage == null) {
      val languageDisplayName = course.languageDisplayName
      return CheckResult(CheckStatus.Unchecked,
                         """Unknown language "$languageDisplayName". Check if support for "$languageDisplayName" is enabled.""")
    }
    val answer = editor.document.text

    val codeSubmission = StepikCheckerConnector.createCodeSubmission(attempt.id, defaultLanguage, answer)
    var submission: Submission = when (val submissionResponse = connector.postSubmission(codeSubmission)) {
      is Err -> return submissionResponse.toCheckResult()
      is Ok -> submissionResponse.value
    }

    val submissionId = submission.id ?: return CheckResult.FAILED_TO_CHECK
    while (submission.status == "evaluation") {
      TimeUnit.MILLISECONDS.sleep(500)
      submission = when (val response = connector.getSubmissionById(submissionId)) {
        is Err -> return response.toCheckResult()
        is Ok -> response.value
      }
    }
    val status = submission.status ?: return CheckResult.FAILED_TO_CHECK
    val isSolved = status != "wrong"
    var message = submission.hint
    if (message == null || message.isEmpty()) {
      message = StringUtil.capitalize(status) + " solution"
    }
    if (isSolved) {
      message = "<html>$message<br/><br/>${EduCoreBundle.message("hyperskill.continue", task.feedbackLink.link!!, EduNames.JBA)}</html>"
    }
    return CheckResult(if (isSolved) CheckStatus.Solved else CheckStatus.Failed, message, needEscape = false)
  }
}

enum class HyperskillLanguages(val id: String?, val langName: String?) {
  JAVA(EduNames.JAVA, "java11"),
  KOTLIN(EduNames.KOTLIN, "kotlin"),
  PYTHON(EduNames.PYTHON, "python3"),
  JAVASCRIPT(EduNames.JAVASCRIPT, "javascript"),
  INVALID(null, null);

  companion object {

    private val titleMap: Map<String?, HyperskillLanguages> by lazy {
      values().associateBy { it.id }
    }

    fun langOfId(lang: String) = titleMap.getOrElse(lang, { INVALID })
  }
}
