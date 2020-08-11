package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.containers.nullize
import com.intellij.util.text.nullize
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getText
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduCoreErrorBundle
import com.jetbrains.edu.learning.stepik.StepikCheckerConnector
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.api.SolutionFile
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLoginListener
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.showErrorDetails
import com.jetbrains.edu.learning.stepik.submissions.SubmissionsManager
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.util.concurrent.TimeUnit

object HyperskillCheckConnector {
  private val LOG = Logger.getInstance(HyperskillCheckConnector::class.java)
  private const val MAX_FILE_SIZE_FOR_PUBLISH = 5 * 1024 * 1024 // 5 Mb
  private val CODE_TASK_CHECK_TIMEOUT = TimeUnit.MINUTES.toSeconds(1)
  const val EVALUATION_STATUS = "evaluation"

  fun postStageSolution(task: Task, project: Project, result: CheckResult) {
    when (val attemptResponse = HyperskillConnector.getInstance().postAttempt(task.id)) {
      is Err -> {
        showErrorDetails(project, attemptResponse.error)
      }
      is Ok -> {
        val feedback = if (result.details == null) result.message else "${result.message}\n${result.details}"
        postEduSubmission(attemptResponse.value, project, task, feedback)
      }
    }
  }

  private fun postEduSubmission(attempt: Attempt, project: Project, task: Task, feedback: String) {
    val files = getSolutionFiles(task, project).nullize() ?: return
    when (val response = HyperskillConnector.getInstance().postSubmission(createEduSubmission(task, attempt, files, feedback))) {
      is Err -> showErrorDetails(project, response.error)
      is Ok -> {
        SubmissionsManager.getInstance(project).addToSubmissionsWithStatus(task.id, task.status, response.value)
        if (task.status == CheckStatus.Solved) {
          val course = task.course as? HyperskillCourse ?: return
          val stage = course.stages.getOrNull(task.index - 1) ?: return
          if (stage.isCompleted) return
          stage.isCompleted = true
          YamlFormatSynchronizer.saveRemoteInfo(course)
        }
      }
    }
  }

  @VisibleForTesting
  fun getSolutionFiles(task: Task, project: Project): List<SolutionFile> {
    val taskDir = task.getDir(project.courseDir)
    if (taskDir == null) {
      val error = EduCoreErrorBundle.message("failed.to.find.dir", task.name)
      LOG.error(error)
      showErrorDetails(project, EduCoreErrorBundle.message("unexpected.error", error))
      return emptyList()
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
        files.add(SolutionFile(taskFile.name, document.text, taskFile.isVisible))
      }
    }
    if (files.isEmpty()) {
      LOG.warn("No files were collected to post solution")
      showErrorDetails(project, EduCoreErrorBundle.message("failed.to.collect.files", task.name))
    }
    return files
  }

  private fun createEduSubmission(task: Task, attempt: Attempt, files: List<SolutionFile>, feedback: String): Submission {
    val score = if (task.status == CheckStatus.Solved) "1" else "0"
    return Submission(score, attempt.id, files, null, feedback)
  }

  private fun String.toCheckResult(): CheckResult {
    return if (this == EduCoreErrorBundle.message("access.denied")) {
      CheckResult(CheckStatus.Unchecked,
                  EduCoreErrorBundle.message("access.denied.with.link"),
                  hyperlinkListener = HyperskillLoginListener
      )
    }
    else CheckResult(CheckStatus.Unchecked, this)
  }

  fun submitCodeTask(project: Project, task: CodeTask): Result<Submission, String> {
    val connector = HyperskillConnector.getInstance()
    val attempt = when (val attemptResponse = connector.postAttempt(task.id)) {
      is Err -> return attemptResponse
      is Ok -> attemptResponse.value
    }

    val course = task.course
    val defaultLanguage = HyperskillLanguages.langOfId(course.languageID).langName
    if (defaultLanguage == null) {
      val languageDisplayName = course.languageDisplayName
      return Err("""Unknown language "$languageDisplayName". Check if support for "$languageDisplayName" is enabled.""")
    }

    val configurator = course.configurator as? HyperskillConfigurator
    val codeTaskText = configurator?.getCodeTaskFile(project, task)?.getText(project)
    if (codeTaskText == null) {
      LOG.error("Unable to create submission: file with code is not found for the task ${task.name}")
      return Err(EduCoreErrorBundle.message("failed.to.post.solution", EduNames.JBA))
    }
    val codeSubmission = StepikCheckerConnector.createCodeSubmission(attempt.id, defaultLanguage, codeTaskText)
    return connector.postSubmission(codeSubmission)
  }


  private fun checkCodeTaskWithWebSockets(project: Project, task: CodeTask): Result<CheckResult, SubmissionError> {
    val connector = HyperskillConnector.getInstance()
    val webSocketConfiguration = connector.getWebSocketConfiguration().onError { error ->
      return Err(SubmissionError.NoSubmission(error))
    }

    val initialState = InitialState(project, task, webSocketConfiguration.token)
    val finalState = connector.connectToWebSocketWithTimeout(CODE_TASK_CHECK_TIMEOUT,
                                                             "${webSocketConfiguration.url}/connection/websocket",
                                                             initialState)

    return finalState.getResult()
  }

  fun checkCodeTask(project: Project, task: CodeTask): CheckResult {
    if (task.id == 0) {
      val link = task.feedbackLink.link ?: return CheckResult.failedToCheck
      val message = """Corrupted task (no id): please, click "Solve in IDE" on <a href="$link">${EduNames.JBA}</a> one more time"""
      return CheckResult(CheckStatus.Unchecked, message)
    }

    return checkCodeTaskWithWebSockets(project, task).onError { submissionError ->
      LOG.info(submissionError.error)
      val submission = when (submissionError) {
        is SubmissionError.NoSubmission -> submitCodeTask(project, task).onError { return it.toCheckResult() }
        is SubmissionError.WithSubmission -> submissionError.submission
      }

      return periodicallyCheckSubmissionResult(project, submission, task)
    }
  }

  private fun periodicallyCheckSubmissionResult(project: Project, submission: Submission, task: CodeTask): CheckResult {
    val submissionId = submission.id!!
    val connector = HyperskillConnector.getInstance()

    var lastSubmission = submission
    var delay = 1L
    val timeout = if (isUnitTestMode) 5L else CODE_TASK_CHECK_TIMEOUT
    while (delay < timeout && lastSubmission.status == EVALUATION_STATUS) {
      TimeUnit.SECONDS.sleep(delay)
      delay *= 2
      lastSubmission = connector.getSubmissionById(submissionId).onError { return it.toCheckResult() }
    }

    if (lastSubmission.status != EVALUATION_STATUS) {
      SubmissionsManager.getInstance(project).addToSubmissions(task.id, lastSubmission)
      return lastSubmission.toCheckResult(task)
    }

    return CheckResult(CheckStatus.Unchecked, EduCoreErrorBundle.message("failed.to.get.check.result.from", EduNames.JBA))
  }
}

fun Submission.toCheckResult(task: Task): CheckResult {
  val status = status ?: return CheckResult.failedToCheck
  val isSolved = status != "wrong"
  var message = hint.nullize() ?: "${StringUtil.capitalize(status)} solution"
  if (isSolved) {
    message = "<html>$message<br/><br/>${EduCoreBundle.message("hyperskill.continue", task.feedbackLink.link!!, EduNames.JBA)}</html>"
  }
  return CheckResult(if (isSolved) CheckStatus.Solved else CheckStatus.Failed, message)
}

enum class HyperskillLanguages(val id: String?, val langName: String?) {
  JAVA(EduNames.JAVA, "java11"),
  KOTLIN(EduNames.KOTLIN, "kotlin"),
  PYTHON(EduNames.PYTHON, "python3"),
  JAVASCRIPT(EduNames.JAVASCRIPT, "javascript"),
  SCALA(EduNames.SCALA, "scala"),
  PLAINTEXT("TEXT", "TEXT"),
  INVALID(null, null);

  companion object {

    private val titleMap: Map<String?, HyperskillLanguages> by lazy {
      values().associateBy { it.id }
    }

    fun langOfId(lang: String) = titleMap.getOrElse(lang, { INVALID })
  }
}
