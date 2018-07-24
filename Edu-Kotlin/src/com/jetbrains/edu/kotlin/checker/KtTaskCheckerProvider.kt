package com.jetbrains.edu.kotlin.checker

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.checker.gradle.*
import com.jetbrains.edu.learning.courseFormat.ext.findTestDir
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
        val testDir = task.findTestDir() ?: error("Failed to find test dir for task ${task.name}")

        var testClasses: List<String>? = null

        ApplicationManager.getApplication().invokeAndWait {
          testClasses = runReadAction {
            testDir.children.mapNotNull {
              val psiFile = PsiManager.getInstance(project).findFile(it) ?: return@mapNotNull null
              KotlinJUnitRunConfigurationProducer.getTestClass(psiFile)?.qualifiedName
            }
          }
        }
        return GradleTask(TEST_TASK_NAME, testClasses?.flatMap { listOf(TESTS_ARG, it) } ?: emptyList())
      }
    }
  }

  override fun mainClassForFile(project: Project, file: VirtualFile): String? {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null
    val ktElements = PsiTreeUtil.findChildrenOfType(psiFile, KtElement::class.java)
    val container = KotlinRunConfigurationProducer.getEntryPointContainer(ktElements.first())
    return KotlinRunConfigurationProducer.getStartClassFqName(container)
  }
}
