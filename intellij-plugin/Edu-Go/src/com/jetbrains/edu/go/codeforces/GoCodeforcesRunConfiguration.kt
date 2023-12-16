package com.jetbrains.edu.go.codeforces

import com.goide.execution.application.GoApplicationConfiguration
import com.goide.psi.GoFile
import com.intellij.execution.InputRedirectAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfigurationType
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.getTaskFile

class GoCodeforcesRunConfiguration(project: Project) :
  GoApplicationConfiguration(project, CodeforcesRunConfigurationType.CONFIGURATION_ID, CodeforcesRunConfigurationType.getInstance()),
  CodeforcesRunConfiguration {
  override fun setExecutableFile(file: VirtualFile) {
    kind = Kind.PACKAGE
    val goPsiFile = PsiManager.getInstance(project).findFile(file) as? GoFile
                    ?: throw IllegalStateException("Unable to find psiFile for virtual file " + file.path)
    val packageName = goPsiFile.getImportPath(false)
                      ?: throw IllegalStateException("Unable to obtain package name for Go file " + file.path)
    setPackage(packageName)
    val taskFile = file.getTaskFile(project) ?: throw IllegalStateException("Unable to find taskFile for virtual file " + file.path)
    val task = taskFile.task
    val taskDir = task.getDir(project.courseDir) ?: throw IllegalStateException("Unable to find taskDir for task " + task.name)
    workingDirectory = taskDir.path
  }

  override fun getInputRedirectOptions(): InputRedirectAware.InputRedirectOptions = this

}
