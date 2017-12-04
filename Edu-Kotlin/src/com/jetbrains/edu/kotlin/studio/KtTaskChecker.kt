package com.jetbrains.edu.kotlin.studio

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.JdkBundle
import com.jetbrains.edu.kotlin.KtTaskChecker
import com.jetbrains.edu.kotlin.KtTaskChecker.FAILED_TO_LAUNCH
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class KtTaskChecker : TaskChecker() {

  override fun isAccepted(task: Task) = task is EduTask && EduUtils.isAndroidStudio()

  override fun check(task: Task, project: Project): CheckResult {
    val cmd = GeneralCommandLine()
    val basePath = project.basePath ?: return FAILED_TO_LAUNCH
    var bundledJavaPath = JdkBundle.getBundledJDKAbsoluteLocation().absolutePath
    if (SystemInfo.isMac) {
      bundledJavaPath = FileUtil.join(PathManager.getHomePath(), "jre", "jdk", "Contents", "Home")
    }
    cmd.withEnvironment("JAVA_HOME", bundledJavaPath)
    val projectPath = FileUtil.toSystemDependentName(basePath)
    cmd.withWorkDirectory(projectPath)
    val executablePath = if (SystemInfo.isWindows) FileUtil.join(projectPath, "gradlew.bat") else "./gradlew"
    cmd.exePath = executablePath
    cmd.addParameter(":lesson${task.lesson.index}:task${task.index}:test")
    return try {
      val output = CheckUtils.getTestOutput(cmd.createProcess(),
              cmd.commandLineString, false)
      CheckResult(if (output.isSuccess) CheckStatus.Solved else CheckStatus.Failed, output.message)
    } catch (e: ExecutionException) {
      Logger.getInstance(KtTaskChecker::class.java).info(CheckAction.FAILED_CHECK_LAUNCH, e)
      FAILED_TO_LAUNCH
    }
  }
}
