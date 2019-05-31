package com.jetbrains.edu.kotlin.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.jvm.gradle.checker.GradleCommandLine
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.jvm.gradle.checker.NewGradleEduTaskChecker
import com.jetbrains.edu.jvm.gradle.checker.hasSeparateModule
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import org.jetbrains.kotlin.idea.run.KotlinJUnitRunConfigurationProducer
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

class KtTaskCheckerProvider : GradleTaskCheckerProvider() {

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
    return object : NewGradleEduTaskChecker(task, project) {

      override fun checkIfFailedToRunTests(): CheckResult {
        return if (task.hasSeparateModule(project)) {
          super.checkIfFailedToRunTests()
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
  }

  override fun mainClassForFile(project: Project, file: VirtualFile): String? {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null
    val ktElements = PsiTreeUtil.findChildrenOfType(psiFile, KtElement::class.java)
    val container = KotlinRunConfigurationProducer.getEntryPointContainer(ktElements.first()) ?: return null
    return KotlinRunConfigurationProducer.getStartClassFqName(container)
  }
}
