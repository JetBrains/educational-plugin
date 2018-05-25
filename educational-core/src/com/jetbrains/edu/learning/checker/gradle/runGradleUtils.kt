package com.jetbrains.edu.learning.checker.gradle

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.checker.CheckUtils.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.dirName
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.intellij.generation.EduGradleUtils
import java.util.regex.Pattern

const val MAIN_CLASS_PROPERTY_PREFIX = "-PmainClass="

private val TEST_FAILED_PATTERN: Pattern = Pattern.compile("(.*)expected:<(.*)> but was:<(.*)>", Pattern.MULTILINE or Pattern.DOTALL)
private val COMPARISON_RANGE_PATTERN: Regex = "\\[(.*)]".toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))

fun getGradleProjectName(task: Task) =
  if (task.lesson.section != null)
    ":${EduGradleUtils.sanitizeName(task.lesson.section!!.name)}-${EduGradleUtils.sanitizeName(task.lesson.name)}-${EduGradleUtils.sanitizeName(task.dirName)}"
  else
    ":${EduGradleUtils.sanitizeName(task.lesson.name)}-${EduGradleUtils.sanitizeName(task.dirName)}"


fun generateGradleCommandLine(project: Project, command: String, vararg additionalParams: String): GeneralCommandLine? {
  val cmd = GeneralCommandLine()
  val basePath = project.basePath ?: return null
  val projectJdkPath = ProjectRootManager.getInstance(project).projectSdk?.homePath ?: return null
  cmd.withEnvironment("JAVA_HOME", projectJdkPath)
  val projectPath = FileUtil.toSystemDependentName(basePath)
  cmd.withWorkDirectory(projectPath)
  val executablePath = if (SystemInfo.isWindows) FileUtil.join(projectPath, "gradlew.bat") else "./gradlew"
  cmd.exePath = executablePath
  cmd.addParameter(command)
  cmd.addParameters(*additionalParams)

  return cmd
}

class GradleOutput(val isSuccess: Boolean, _messages: List<String>) {
  val messages = _messages.map { it.postProcessOutput() }

  val firstMessage: String get() = messages.firstOrNull { it.isNotBlank() } ?: "<no output>"
}

fun getProcessOutput(process: Process, commandLine: String, taskName: String): GradleOutput {
  val handler = CapturingProcessHandler(process, null, commandLine)
  val output =
    if (ProgressManager.getInstance().hasProgressIndicator()) {
      handler.runProcessWithProgressIndicator(ProgressManager.getInstance().progressIndicator)
    }
    else {
      handler.runProcess()
    }

  val stderr = output.stderr
  if (!stderr.isEmpty() && output.stdout.isEmpty()) {
    return GradleOutput(false, listOf(stderr))
  }

  //gradle prints compilation failures to error stream
  if (hasCompilationErrors(output)) {
    return GradleOutput(false, listOf(COMPILATION_FAILED_MESSAGE))
  }

  if (!output.stdout.contains(taskName)) {
    TaskChecker.LOG.warn("#educational: executing $taskName fails: \n" + output.stdout)
    return GradleOutput(false, listOf("$FAILED_TO_CHECK_MESSAGE. See idea.log for more details."))
  }

  var currentMessage: StringBuilder? = null
  val allMessages = mutableListOf<String>()

  fun addCurrentMessageIfNeeded() {
    if (currentMessage != null) {
      allMessages += currentMessage.toString()
    }
  }

  for (line in output.stdoutLines) {
    if (line.startsWith(STUDY_PREFIX)) {
      val messageLine = line.removePrefix(STUDY_PREFIX)
      if (currentMessage != null) {
        currentMessage.appendln(messageLine)
      } else {
        currentMessage = StringBuilder(messageLine).append("\n")
      }
    } else {
      addCurrentMessageIfNeeded()
      currentMessage = null
    }
  }

  addCurrentMessageIfNeeded()

  return GradleOutput(true, allMessages)
}

fun String.postProcessOutput() = replace(System.getProperty("line.separator"), "\n").removeSuffix("\n")

fun parseTestsOutput(process: Process, commandLine: String, taskName: String): CheckResult {
  val output = getProcessOutput(process, commandLine, taskName)
  if (!output.isSuccess) return CheckResult(CheckStatus.Failed, output.firstMessage)

  var congratulations = TestsOutputParser.CONGRATULATIONS

  loop@for (message in output.messages) {
    when {
      TestsOutputParser.TEST_OK in message -> continue@loop
      TestsOutputParser.CONGRATS_MESSAGE in message -> {
        congratulations = message.substringAfter(TestsOutputParser.CONGRATS_MESSAGE)
      }
      TestsOutputParser.TEST_FAILED in message -> {
        return CheckResult(CheckStatus.Failed, message.substringAfter(TestsOutputParser.TEST_FAILED).prettify())
      }
    }
  }

  return CheckResult(CheckStatus.Solved, congratulations)
}

private fun String.prettify(): String {
  val matcher = TEST_FAILED_PATTERN.matcher(this)
  return if (matcher.find()) {
    val errorMessage = matcher.group(1)
    val expectedText = matcher.group(2).replace(COMPARISON_RANGE_PATTERN, "$1")
    val actualText = matcher.group(3).replace(COMPARISON_RANGE_PATTERN, "$1")
    "$errorMessage\nExpected: $expectedText\nActual: $actualText"
  } else {
    this
  }
}

/**
 * Run gradle 'run' task.
 * Returns gradle output if task was successfully executed, otherwise returns CheckResult.
 */
fun runGradleRunTask(project: Project, task: Task,
                     mainClassForFile: (Project, VirtualFile) -> String?): ExecutionResult<String, CheckResult> {
  val mainClassName = findMainClass(project, task, mainClassForFile)
          ?: return Err(CheckResult(CheckStatus.Unchecked, "Unable to execute task ${task.name}"))
  val taskName = "${getGradleProjectName(task)}:run"
  val cmd = generateGradleCommandLine(
    project,
    taskName,
    "${MAIN_CLASS_PROPERTY_PREFIX}$mainClassName"
  ) ?: return Err(CheckResult.FAILED_TO_CHECK)

  val gradleOutput = getProcessOutput(cmd.createProcess(), cmd.commandLineString, taskName)
  if (!gradleOutput.isSuccess) {
    return Err(CheckResult(CheckStatus.Failed, gradleOutput.firstMessage))
  }

  return Ok(gradleOutput.firstMessage)
}

private fun findMainClass(project: Project, task: Task, mainClassForFile: (Project, VirtualFile) -> String?): String? =
  runReadAction {
    val selectedFile = getSelectedFile(project)
    if (selectedFile != null) {
      val fileTask = EduUtils.getTaskForFile(project, selectedFile)
      if (fileTask == task) {
        val mainClass = mainClassForFile(project, selectedFile)
        if (mainClass != null) return@runReadAction mainClass
      }
    }

    val taskDir = task.getTaskDir(project) ?: return@runReadAction null

    for ((name, _) in task.taskFiles) {
      val file = taskDir.findChild(name) ?: continue
      return@runReadAction mainClassForFile(project, file) ?: continue
    }
    null
  }

private fun getSelectedFile(project: Project): VirtualFile? {
  val editor = EduUtils.getSelectedEditor(project) ?: return null
  return FileDocumentManager.getInstance().getFile(editor.document)
}
