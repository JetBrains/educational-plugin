@file:Suppress("HardCodedStringLiteral")

package com.jetbrains.edu.learning.configuration

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.checker.CodeExecutor
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.shouldBeEmpty
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import java.io.IOException
import javax.swing.Icon


open class PlainTextConfigurator : EduConfigurator<Unit> {
  override val courseBuilder: EduCourseBuilder<Unit>
    get() = PlainTextCourseBuilder()

  override val testFileName: String
    get() = "Tests.txt"

  override fun getMockFileName(text: String): String = "Task.txt"

  override val testDirs: List<String>
    get() = listOf(TEST_DIR_NAME)

  override val isEnabled: Boolean
    get() = ApplicationManager.getApplication().isInternal || isUnitTestMode

  override val logo: Icon
    get() = AllIcons.FileTypes.Text

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
  override val taskCheckerProvider
    get() = object : TaskCheckerProvider {
      override val codeExecutor: CodeExecutor
        get() {
          return object : CodeExecutor {
            override fun execute(project: Project, task: Task, indicator: ProgressIndicator, input: String?): Result<String, CheckResult> {
              val taskDir = task.getDir(project.courseDir) ?: return Err(CheckResult(CheckStatus.Unchecked, "No taskDir in tests"))
              val checkResultFile = taskDir.findChild(CHECK_RESULT_FILE)
                                    ?: return Err(CheckResult(CheckStatus.Unchecked, "No $CHECK_RESULT_FILE file"))
              return Ok(VfsUtil.loadText(checkResultFile))
            }
          }
        }

      override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
        return object : TaskChecker<EduTask>(task, project) {
          override fun check(indicator: ProgressIndicator): CheckResult {
            val taskDir = task.getDir(project.courseDir) ?: error("No taskDir in tests")
            val checkResultFile = taskDir.findChild(CHECK_RESULT_FILE)

            val testFiles = task.taskFiles.values.filter { task.shouldBeEmpty(it.name) }
            for (testFile in testFiles) {
              val vTestFile = taskDir.findFileByRelativePath(testFile.name) ?: error("No virtual test file found")
              invokeAndWaitIfNeeded {
                if (testFile.text != vTestFile.document.text) {
                  error("Saved text for the test file doesn't match actual text")
                }
              }
            }

            if (checkResultFile == null) {
              return CheckResult.SOLVED
            }
            return checkResultFile.checkResult
          }
        }
      }
    }

  private val VirtualFile.checkResult: CheckResult
    get() = try {
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

  override val isCourseCreatorEnabled: Boolean
    get() = ApplicationManager.getApplication().isInternal || isUnitTestMode

  companion object {
    const val CHECK_RESULT_FILE = "checkResult.txt"
    const val TEST_DIR_NAME = "tests"
  }
}

class PlainTextCourseBuilder : EduCourseBuilder<Unit> {
  override val taskTemplateName: String = "Task.txt"
  override val mainTemplateName: String = "Main.txt"
  override val testTemplateName: String = "Tests.txt"

  override fun getLanguageSettings(): LanguageSettings<Unit> = object : LanguageSettings<Unit>() {
    override fun getSettings() {}
  }

  override fun getSupportedLanguageVersions(): List<String> = listOf("1.42")

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<Unit> = PlainTextCourseGenerator(this, course)
}

class PlainTextCourseGenerator(builder: EduCourseBuilder<Unit>, course: Course) : CourseProjectGenerator<Unit>(builder, course)
