package com.jetbrains.edu.kotlin.checker

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.jetbrains.edu.jvm.gradle.checker.*
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.ext.findTestDirs
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import org.jetbrains.kotlin.idea.run.KotlinJUnitRunConfigurationProducer

class KtGradleTaskChecker(task: EduTask, project: Project) : GradleEduTaskChecker(task, project) {

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

    return if (testClasses.isEmpty()) {
      TaskChecker.LOG.warn("Can't find any test class. Check course project is compilable")
      GradleTask(ASSEMBLE_TASK_NAME)
    } else {
      GradleTask(TEST_TASK_NAME, testClasses.flatMap { listOf(TESTS_ARG, it) })
    }
  }
}
