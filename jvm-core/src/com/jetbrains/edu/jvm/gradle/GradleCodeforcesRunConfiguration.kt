package com.jetbrains.edu.jvm.gradle

import com.intellij.execution.Executor
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.jetbrains.edu.jvm.MainFileProvider.Companion.getMainClass
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfigurationType
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.languageById

class GradleCodeforcesRunConfiguration(project: Project, factory: ConfigurationFactory) :
  ApplicationConfiguration(CodeforcesRunConfigurationType.CONFIGURATION_ID, project, factory),
  CodeforcesRunConfiguration {
  override fun setExecutableFile(file: VirtualFile) {
    val course = project.course
    if (course == null) {
      LOG.error("Unable to find course from gradle run configuration")
      return
    }
    val language = course.languageById
    if (language == null) {
      LOG.error("Unable to get language for course " + course.presentableName)
      return
    }
    val mainClass = getMainClass(project, file, language) as PsiClass?
    if (mainClass == null) {
      LOG.error("Unable to find main class for file " + file.path)
      return
    }
    setMainClass(mainClass)
  }

  // TODO: remove this after input file substitution doesn't depend on id in the platform
  // This method overriding is needed because currently input for java is redirected for specific configurations only
  // see com.intellij.execution.InputRedirectAware.TYPES_WITH_REDIRECT_AWARE_UI
  override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState {
    val state = getJavaApplicationCommandLineState(this, env)
    val module = configurationModule
    state.consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project, module.searchScope)
    return state
  }

  companion object {
    private val LOG = logger<GradleCodeforcesRunConfiguration>()
  }
}
