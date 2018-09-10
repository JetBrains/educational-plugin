package com.jetbrains.edu.learning.coursera

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.remote.RemoteTaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.apache.commons.codec.binary.Base64
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder

class CourseraTaskChecker : RemoteTaskChecker {
  private val checkWithoutCredentials = CheckResult(CheckStatus.Unchecked, "Can't check on remote without credentials")
  override fun canCheck(project: Project, task: Task) =
    EduUtils.isStudentProject(project) && CourseraNames.COURSE_TYPE == task.course.courseType


  override fun check(project: Project, task: Task): CheckResult {
    val courseraSettings = CourseraSettings.getInstance()
    if (!courseraSettings.hasFullCredentials()) {
      askToEnterCredentials()

      if (!courseraSettings.hasFullCredentials()) {
        return checkWithoutCredentials
      }
    }

    val taskDir = task.getDir(project) ?: error("No directory for task ${task.name}")

    val assignmentKey = taskDir.getValueFromChildFile("assignmentKey")
    val partId = taskDir.getValueFromChildFile("partId")

    val output = task.taskFiles.mapValues {
      val file = it.value.getVirtualFile(project) ?: error("VirtualFile for ${it.key} not found")
      Base64.encodeBase64String(VfsUtilCore.loadBytes(file))
    }
    val submission = Submission(assignmentKey, courseraSettings.email!!, courseraSettings.token!!,
                                mapOf(Pair(partId, Part(ObjectMapper().writeValueAsString(output)))))
    val json = ObjectMapper().writeValueAsString(submission)

    val client = HttpClientBuilder.create().build()
    val post = HttpPost("https://www.coursera.org/api/onDemandProgrammingScriptSubmissions.v1")
    post.entity = StringEntity(json, ContentType.APPLICATION_JSON)
    val response = client.execute(post)
    if (response.statusLine.statusCode == HttpStatus.SC_CREATED) {
      return CheckResult(CheckStatus.Unchecked, "Submission successful, please check on the coursera grader page for the status");
    }
    return CheckResult.FAILED_TO_CHECK
  }

  private fun askToEnterCredentials() {
    val courseraSettings = CourseraSettings.getInstance()

    val emailField = JBTextField(courseraSettings.email)
    val tokenField = JBTextField(courseraSettings.token)
    val credentialsPanel = panel {
      row("Email:") { emailField(growPolicy = GrowPolicy.MEDIUM_TEXT) }
      row("Token:") { tokenField(growPolicy = GrowPolicy.MEDIUM_TEXT) }
    }
    var refusedToProvidedCredentials = false

    ApplicationManager.getApplication().invokeAndWait {
      refusedToProvidedCredentials = !DialogBuilder().centerPanel(credentialsPanel).showAndGet()
    }

    if (!refusedToProvidedCredentials) {
      CourseraSettings.getInstance().email = emailField.text
      CourseraSettings.getInstance().token = tokenField.text
    }
  }

  private fun VirtualFile.getValueFromChildFile(fileName: String): String {
    val file = this.findChild(fileName) ?: error("$fileName not found in ${this.path}")
    return VfsUtil.loadText(file)
  }

  private fun CourseraSettings.hasFullCredentials() = email.isNotEmpty() && token.isNotEmpty()
}