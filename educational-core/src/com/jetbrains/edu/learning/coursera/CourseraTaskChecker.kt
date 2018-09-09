package com.jetbrains.edu.learning.coursera

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
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
  override fun canCheck(project: Project, task: Task) =
    EduUtils.isStudentProject(project) && CourseraNames.COURSE_TYPE == task.course.courseType

  override fun check(project: Project, task: Task): CheckResult {
    //TODO: don't ask for email and token every time
    val email = Ref<String>()
    val secret = Ref<String>()
    ApplicationManager.getApplication().invokeAndWait {
      email.set(Messages.showInputDialog("Email:", "", null))
      if (email.get() == null) {
        return@invokeAndWait
      }
      secret.set(Messages.showInputDialog("Coursera Token:", "", null))
    }
    if (email.get() == null || secret.get() == null) {
      return CheckResult(CheckStatus.Unchecked, "Can't check on remote without credentials")
    }

    val taskDir = task.getDir(project) ?: error("No directory for task ${task.name}")

    //TODO: add to format
    val assignmentKey = taskDir.getValueFromChildFile("assignmentKey")
    val partId = taskDir.getValueFromChildFile("partId")

    val output = task.taskFiles.mapValues {
      val file = it.value.getVirtualFile(project) ?: error("VirtualFile for ${it.key} not found")
      Base64.encodeBase64String(VfsUtilCore.loadBytes(file))
    }
    val submission = Submission(assignmentKey, email.get(), secret.get(), mapOf(Pair(partId, Part(ObjectMapper().writeValueAsString(output)))))
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

  private fun VirtualFile.getValueFromChildFile(fileName: String): String {
    val file = this.findChild(fileName) ?: error("$fileName not found in ${this.path}")
    return VfsUtil.loadText(file)
  }
}