package com.jetbrains.edu.kotlin.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.jetbrains.edu.jvm.gradle.checker.GradleCommandLine
import com.jetbrains.edu.jvm.gradle.checker.NewGradleEduTaskChecker
import com.jetbrains.edu.jvm.gradle.checker.hasSeparateModule
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import org.jetbrains.kotlin.idea.run.KotlinJUnitRunConfigurationProducer
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

class KtNewGradleTaskChecker(task: EduTask, project: Project) : NewGradleEduTaskChecker(task, project) {

  override fun computePossibleErrorResult(stderr: String): CheckResult {
    return if (task.hasSeparateModule(project)) {
      super.computePossibleErrorResult(stderr)
    } else  {
      GradleCommandLine.create(project, "testClasses")?.launchAndCheck() ?: CheckResult.FAILED_TO_CHECK
    }
  }

  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    val testConfigurations = super.createTestConfigurations()
    // Android Studio produces configurations that correctly work even in one module project
    if (EduUtils.isAndroidStudio() || task.hasSeparateModule (project) || testConfigurations.size != 1) {
      return testConfigurations
    }

    val configuration = testConfigurations.single().configuration as? GradleRunConfiguration ?: return emptyList()

    val testDirs = task.findTestDirs(project)
    check(testDirs.isNotEmpty()) {
      error("Failed to find test dirs for task ${task.name}")
    }

    val testClasses: List<String> = testDirs.flatMap { testDir ->
      testDir.children.mapNotNull {
        val psiFile = PsiManager.getInstance(project).findFile(it) ?: return@mapNotNull null
        KotlinJUnitRunConfigurationProducer.getTestClass(psiFile)?.qualifiedName
      }
    }

    configuration.settings.scriptParameters = testClasses.joinToString(" ") { "--tests $it" }
    return testConfigurations
  }
}
