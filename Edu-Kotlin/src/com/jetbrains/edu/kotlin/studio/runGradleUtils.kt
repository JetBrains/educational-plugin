package com.jetbrains.edu.kotlin.studio

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.JdkBundle
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckUtils.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer
import org.jetbrains.kotlin.psi.KtElement

val MAIN_CLASS_PROPERTY_PREFIX = "-PmainClass="

fun getGradleProjectName(task: Task) = ":lesson${task.lesson.index}:task${task.index}"

fun generateGradleCommandLine(project: Project, command: String, vararg additionalParams: String): GeneralCommandLine? {
    val cmd = GeneralCommandLine()
    val basePath = project.basePath ?: return null
    var bundledJavaPath = JdkBundle.getBundledJDKAbsoluteLocation().absolutePath
    if (SystemInfo.isMac) {
        bundledJavaPath = FileUtil.join(PathManager.getHomePath(), "jre", "jdk", "Contents", "Home")
    }
    cmd.withEnvironment("JAVA_HOME", bundledJavaPath)
    val projectPath = FileUtil.toSystemDependentName(basePath)
    cmd.withWorkDirectory(projectPath)
    val executablePath = if (SystemInfo.isWindows) FileUtil.join(projectPath, "gradlew.bat") else "./gradlew"
    cmd.exePath = executablePath
    cmd.addParameter(command)
    cmd.addParameters(*additionalParams)

    return cmd
}

fun getProcessOutput(process: Process, commandLine: String): String {
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
        return stderr
    }

    //gradle prints compilation failures to error stream
    if (hasCompilationErrors(output)) {
        return COMPILATION_FAILED_MESSAGE
    }

    val sb = StringBuilder()
    output.stdoutLines.forEach {
        if (it.startsWith(STUDY_PREFIX)) sb.appendln(it.removePrefix(STUDY_PREFIX))
    }

    return sb.toString()
}

fun String.postProcessOutput() = this.replace(System.getProperty("line.separator"), "\n").removeSuffix("\n")

fun getMainClassName(project: Project): String? {
    return ApplicationManager.getApplication().runReadAction(Computable {
        val editor = EduUtils.getSelectedEditor(project) ?: return@Computable null
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return@Computable null
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@Computable null

        val ktElements = PsiTreeUtil.findChildrenOfType(psiFile, KtElement::class.java)
        val container = KotlinRunConfigurationProducer.getEntryPointContainer(ktElements.first())
        return@Computable KotlinRunConfigurationProducer.getStartClassFqName(container)
    })
}


private fun hasCompilationErrors(processOutput: ProcessOutput): Boolean {
    return ContainerUtil.find(processOutput.stderrLines) { line -> line.contains(COMPILATION_ERROR) } != null
}