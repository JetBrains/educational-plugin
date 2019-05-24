package com.jetbrains.edu.learning.stepik.hyperskill

import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikCheckerConnector
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConnector.getSubmissionById
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConnector.postAttempt
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConnector.postSubmission
import java.util.concurrent.TimeUnit

object HyperskillCheckConnector {
  private val LOG = Logger.getInstance(HyperskillCheckConnector::class.java)

  fun postSolution(task: Task, project: Project, result: CheckResult) {
    val attempt = postAttempt(task.id)
    if (attempt == null) {
      showFailedToPostNotification()
      return LOG.error("Failed to post attempt for stage ${task.id}")
    }
    postEduSubmission(attempt, project, task, "${result.message}\n${result.details}")
  }

  private fun postEduSubmission(attempt: Attempt, project: Project, task: Task, feedback: String) {
    val taskDir = task.getTaskDir(project) ?: return LOG.error("Failed to find stage directory ${task.name}")

    val files = ArrayList<SolutionFile>()
    for (taskFile in task.taskFiles.values) {
      val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
      runReadAction {
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return@runReadAction
        files.add(SolutionFile(taskFile.name, document.text))
      }
    }

    val submission = createEduSubmission(task, attempt, files, feedback)
    postSubmission(submission)
  }

  private fun createEduSubmission(task: Task, attempt: Attempt, files: ArrayList<SolutionFile>, feedback: String): Submission {
    val score = if (task.status == CheckStatus.Solved) "1" else "0"
    val objectMapper = StepikConnector.createMapper(SimpleModule())
    val serializedTask = objectMapper.writeValueAsString(TaskData(task))
    return Submission(score, attempt.id, files, serializedTask, feedback)
  }

  fun checkCodeTask(project: Project, task: Task): CheckResult {
    val attempt = postAttempt(task.id) ?: return CheckResult.FAILED_TO_CHECK
    val course = task.lesson.course
    val courseLanguage = course.languageById
    val editor = EduUtils.getSelectedEditor(project)
    if (editor != null) {
      val commentPrefix = LanguageCommenters.INSTANCE.forLanguage(courseLanguage!!).lineCommentPrefix
      val answer = commentPrefix + StepikCheckerConnector.EDU_TOOLS_COMMENT + editor.document.text
      val defaultLanguage = HyperskillLanguages.langOfId(courseLanguage.id).langName
                            ?: return CheckResult(CheckStatus.Unchecked, "Language not found for: " + courseLanguage.displayName)

      val codeSubmission = StepikCheckerConnector.createCodeSubmission(attempt.id, defaultLanguage, answer)
      var submission : Submission? = postSubmission(codeSubmission) ?: return CheckResult.FAILED_TO_CHECK
      if (submission == null) return CheckResult.FAILED_TO_CHECK

      val submissionId = submission.id ?: return CheckResult.FAILED_TO_CHECK
      while (submission != null && "evaluation" == submission.status) {
        TimeUnit.MILLISECONDS.sleep(500)
        submission = getSubmissionById(submissionId)
      }
      if (submission == null) return CheckResult.FAILED_TO_CHECK
      val status = submission.status ?: return CheckResult.FAILED_TO_CHECK
      val isSolved = status != "wrong"
      var message = submission.hint
      if (message == null || message.isEmpty()) {
        message = StringUtil.capitalize(status) + " solution"
      }
      if (isSolved) {
        message = "<html> $message <br/><br/> $CONTINUE_ON_HYPERSKILL</html>"
      }
      return CheckResult(if (isSolved) CheckStatus.Solved else CheckStatus.Failed, message, needEscape = false)
    }
    return CheckResult.FAILED_TO_CHECK
  }
}

enum class HyperskillLanguages (val id: String?, val langName: String?) {
  JAVA(EduNames.JAVA, "java11"),
  KOTLIN(EduNames.KOTLIN, "kotlin"),
  PYTHON(EduNames.PYTHON, "python3"),
  INVALID(null, null);

  companion object {

    private val titleMap: Map<String?, HyperskillLanguages> by lazy {
      values().associateBy { it.id }
    }

    fun langOfId(lang: String) = titleMap.getOrElse(lang, { INVALID })
  }
}
