package com.jetbrains.edu.kotlin.checker

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.checker.gradle.*
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import org.jetbrains.kotlin.idea.run.KotlinJUnitRunConfigurationProducer
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer
import org.jetbrains.kotlin.psi.KtElement

class KtTaskCheckerProvider : GradleTaskCheckerProvider() {

  override fun getEduTaskChecker(task: EduTask, project: Project): GradleEduTaskChecker {
    return object : GradleEduTaskChecker(task, project) {

      override fun getGradleTask(): GradleTask {
        if (task.hasSeparateModule(project)) {
          return super.getGradleTask()
        }

        val testDirs = task.findTestDirs(project)
        check(testDirs.isNotEmpty()) {
          error("Failed to find test dirs for task ${task.name}")
        }

        val testClasses: List<String> = testDirs.flatMap { testDir ->
          var tests: List<String>? = null
          ApplicationManager.getApplication().invokeAndWait {
             tests = runReadAction {
              testDir.children.mapNotNull {
                val psiFile = PsiManager.getInstance(project).findFile(it) ?: return@mapNotNull null
                KotlinJUnitRunConfigurationProducer.getTestClass(psiFile)?.qualifiedName
              }
            }
          }
          tests ?: emptyList()
        }

        return GradleTask(TEST_TASK_NAME, testClasses.flatMap { listOf(TESTS_ARG, it) })
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
