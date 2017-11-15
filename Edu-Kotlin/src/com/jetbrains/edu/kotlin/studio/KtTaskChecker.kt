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
import com.jetbrains.edu.learning.actions.StudyCheckAction
import com.jetbrains.edu.learning.checker.StudyCheckResult
import com.jetbrains.edu.learning.checker.StudyCheckUtils
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.StudyStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class KtTaskChecker(task: EduTask, project: Project) : TaskChecker<EduTask>(task, project) {
  override fun check(): StudyCheckResult {
    val cmd = GeneralCommandLine()
    val basePath = myProject.basePath ?: return FAILED_TO_LAUNCH
    var bundledJavaPath = JdkBundle.getBundledJDKAbsoluteLocation().absolutePath
    if (SystemInfo.isMac) {
      bundledJavaPath = FileUtil.join(PathManager.getHomePath(), "jre", "jdk", "Contents", "Home")
    }
    cmd.withEnvironment("JAVA_HOME", bundledJavaPath)
    val projectPath = FileUtil.toSystemDependentName(basePath)
    cmd.withWorkDirectory(projectPath)
    val executablePath = if (SystemInfo.isWindows) FileUtil.join(projectPath, "gradlew.bat") else "./gradlew"
    cmd.exePath = executablePath
    cmd.addParameter(":lesson${myTask.lesson.index}:task${myTask.index}:test")
    return try {
      val output = StudyCheckUtils.getTestOutput(cmd.createProcess(),
              cmd.commandLineString, false)
      StudyCheckResult(if (output.isSuccess) StudyStatus.Solved else StudyStatus.Failed, output.message)
    } catch (e: ExecutionException) {
      Logger.getInstance(KtTaskChecker::class.java).info(StudyCheckAction.FAILED_CHECK_LAUNCH, e)
      FAILED_TO_LAUNCH
    }
  }
}
