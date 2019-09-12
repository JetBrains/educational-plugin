package com.jetbrains.edu.learning.configuration

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import java.io.IOException
import javax.swing.Icon

open class PlainTextConfigurator : EduConfigurator<Unit> {

  override fun getCourseBuilder() = PlainTextCourseBuilder()
  override fun getTestFileName() = "Tests.txt"
  override fun getMockFileName(text: String): String = "Task.txt"

  override fun getTestDirs() = listOf("tests")

  override fun getLogo(): Icon = AllIcons.FileTypes.Text

  /**
   * To specify check result for plain text courses in tests:
   * 1. Add file called [CHECK_RESULT_FILE] to a task.
   * 2. [CHECK_RESULT_FILE] is used to determine check result.
   * 3. Check result is specified in the following format: `status message`
   *
   * **Examples**:
   *
   * `Solved Great job!`
   *
   * `Failed Incorrect`
   *
   * `Unchecked Failed to check`*
   */
  override fun getTaskCheckerProvider() = TaskCheckerProvider { task, project ->
    object : TaskChecker<EduTask>(task, project) {
      override fun check(indicator: ProgressIndicator): CheckResult {
        val taskDir = task.getDir(project) ?: error("No taskDir in tests")
        val checkResultFile = taskDir.findChild(CHECK_RESULT_FILE)
        if (checkResultFile == null) {
          return CheckResult.SOLVED
        }
        return checkResultFile.checkResult
      }
    }
  }

  private val VirtualFile.checkResult: CheckResult
    get() =
      try {
        val text = VfsUtil.loadText(this)
        val statusWithMessage = text.split(" ", limit = 2)
        val message = if (statusWithMessage.size > 1) statusWithMessage[1] else ""
        CheckResult(CheckStatus.valueOf(statusWithMessage[0]), message)
      }
      catch (e: IOException) {
        CheckResult.SOLVED
      }
      catch (e: IllegalArgumentException) {
        CheckResult.SOLVED
      }

  override fun isCourseCreatorEnabled(): Boolean = ApplicationManager.getApplication().isInternal || isUnitTestMode

  companion object {
    const val CHECK_RESULT_FILE = "checkResult.txt"
  }
}

class PlainTextCourseBuilder : EduCourseBuilder<Unit> {
  override fun getLanguageSettings(): LanguageSettings<Unit> = object : LanguageSettings<Unit>() {
    override fun getSettings() {}
    override fun getLanguageVersions() = mutableListOf("1.42")
  }
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<Unit> = PlainTextCourseGenerator(this, course)
  override fun getTaskTemplateName(): String? = "Task.txt"
  override fun getTestTemplateName(): String? = "Tests.txt"
}

class PlainTextCourseGenerator(builder: EduCourseBuilder<Unit>, course: Course) : CourseProjectGenerator<Unit>(builder, course)
