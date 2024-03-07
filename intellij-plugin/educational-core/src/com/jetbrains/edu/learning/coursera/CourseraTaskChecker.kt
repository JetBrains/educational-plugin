package com.jetbrains.edu.learning.coursera

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.annotations.VisibleForTesting
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.checker.remote.RemoteTaskChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseraCourse
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.loadEncodedContent
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.network.addProxy
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import java.time.Duration

class CourseraTaskChecker : RemoteTaskChecker {
  private val checkWithoutCredentials = CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("coursera.error.no.credentials"))
  override fun canCheck(project: Project, task: Task) =
    project.isStudentProject() && task.course is CourseraCourse

  override fun check(project: Project, task: Task, indicator: ProgressIndicator): CheckResult {
    val course = task.course as CourseraCourse
    if (course.submitManually) {
      return CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("coursera.local.tests.passed", getLinkToSubmission(task)))
    }
    val courseraSettings = CourseraSettings.getInstance()
    var askedForCredentials = false
    var token = getToken()
    if (courseraSettings.email.isEmpty() || token == null) {
      askToEnterCredentials(task)
      askedForCredentials = true
      token = getToken()
      if (courseraSettings.email.isEmpty() || token == null) {
        return checkWithoutCredentials
      }
    }

    return try {
      val response = postSubmission(createSubmissionJson(project, task, courseraSettings, token))
      var responseCode = response.code
      if (responseCode != HttpStatus.SC_CREATED && !askedForCredentials) {
        askToEnterCredentials(task, createCheckResult(responseCode, task).message)
        token = getToken()
        if (token != null) {
          responseCode = postSubmission(createSubmissionJson(project, task, courseraSettings, token)).code
        }
      }
      createCheckResult(responseCode, task)
    }
    catch (e: Exception) {
      Logger.getInstance(CourseraTaskChecker::class.java).warn(e)
      CheckResult.CONNECTION_FAILED
    }
  }

  private fun createCheckResult(statusCode: Int, task: Task): CheckResult {
    return when (statusCode) {
      HttpStatus.SC_CREATED -> CheckResult(CheckStatus.Unchecked,
                                           EduCoreBundle.message("coursera.successful.submission", getLinkToSubmission(task)))
      HttpStatus.SC_UNAUTHORIZED -> CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("coursera.error.invalid.credentials"))
      HttpStatus.SC_BAD_REQUEST -> CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("coursera.error.invalid.token"))
      else -> CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("coursera.error.failed.creating.submission", statusCode))
    }
  }

  private fun getLinkToSubmission(task: Task): String {
    return task.feedbackLink?.replace("/discussions", "/submission") ?: "https://www.coursera.org/"
  }

  private fun getLinkToToken(task: Task): String {
    return task.feedbackLink?.removeSuffix("/discussions") ?: "https://www.coursera.org/"
  }

  @VisibleForTesting
  fun createSubmissionJson(project: Project, task: Task, courseraSettings: CourseraSettings, token: String): String {
    val taskDir = task.getDir(project.courseDir) ?: error("No directory for task ${task.name}")

    val assignmentKey = taskDir.getValueFromChildFile(ASSIGNMENT_KEY)
    val partId = taskDir.getValueFromChildFile(PART_ID)

    val output = task.taskFiles.filterValues { it.name != PART_ID && it.name != ASSIGNMENT_KEY }.mapValues {
      val file = it.value.getVirtualFile(project) ?: error("VirtualFile for ${it.key} not found")
      file.loadEncodedContent(isToEncodeContent = true)
    }
    val submission = Submission(assignmentKey, courseraSettings.email, token,
                                mapOf(Pair(partId, Part(ObjectMapper().writeValueAsString(output)))))
    return ObjectMapper().writeValueAsString(submission)
  }

  private fun postSubmission(json: String): Response {
    val builder = OkHttpClient.Builder()
      .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS.toLong()))
      .callTimeout(Duration.ofSeconds(TIMEOUT_SECONDS.toLong()))
      .addProxy(ON_DEMAND_SUBMIT)

    val request = Request.Builder()
      .url(ON_DEMAND_SUBMIT)
      .method("POST", json.toRequestBody(ContentType.APPLICATION_JSON.mimeType.toMediaType())).build()

    return builder.build().newCall(request).execute()
  }

  private fun askToEnterCredentials(task: Task, message: String? = null) {
    val courseraSettings = CourseraSettings.getInstance()

    val emailField = JBTextField(courseraSettings.email)
    val tokenField = JBTextField(getToken())
    val credentialsPanel = panel {
      if (message != null) {
        val messageLabel = JBLabel(message)
        messageLabel.foreground = JBColor.RED
        messageLabel.withFont(JBUI.Fonts.label().asBold())
        row { cell(messageLabel) }
      }
      row("${EduCoreBundle.message("label.coursera.email")}:") {
        cell(emailField)
          .columns(COLUMNS_MEDIUM)
      }
      row("${EduCoreBundle.message("label.coursera.token")}:") {
        cell(tokenField)
          .columns(COLUMNS_MEDIUM)
      }
      row {
        comment(EduCoreBundle.message("label.coursera.obtain.token", getLinkToToken(task)))
      }
    }
    var refusedToProvideCredentials = false

    ApplicationManager.getApplication().invokeAndWait {
      refusedToProvideCredentials = !DialogBuilder().centerPanel(credentialsPanel)
        .title(EduCoreBundle.message("dialog.title.coursera.credentials"))
        .showAndGet()
    }

    if (!refusedToProvideCredentials) {
      val credentialAttributes = credentialAttributes(emailField.text)
      PasswordSafe.instance.set(credentialAttributes, Credentials(emailField.text, tokenField.text))
      CourseraSettings.getInstance().email = emailField.text
    }
  }

  private fun VirtualFile.getValueFromChildFile(fileName: String): String {
    val file = this.findChild(fileName) ?: error("$fileName not found in ${this.path}")
    return VfsUtil.loadText(file)
  }

  private fun credentialAttributes(email: String) =
    CredentialAttributes(generateServiceName(SERVICE_DISPLAY_NAME, email))

  private fun getToken(): String? {
    return PasswordSafe.instance.get(credentialAttributes(CourseraSettings.getInstance().email))?.getPasswordAsString()
  }

  companion object {
    private const val ON_DEMAND_SUBMIT = "https://www.coursera.org/api/onDemandProgrammingScriptSubmissions.v1"
    private const val TIMEOUT_SECONDS = 10

    @VisibleForTesting
    const val ASSIGNMENT_KEY = "assignmentKey"

    @VisibleForTesting
    const val PART_ID = "partId"

    @Suppress("UnstableApiUsage")
    @NlsSafe
    private const val SERVICE_DISPLAY_NAME = "EduTools Coursera Integration"
  }
}