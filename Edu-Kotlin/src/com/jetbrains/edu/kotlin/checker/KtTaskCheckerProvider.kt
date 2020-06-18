package com.jetbrains.edu.kotlin.checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import org.jetbrains.kotlin.idea.isMainFunction
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer
import org.jetbrains.kotlin.psi.KtElement

class KtTaskCheckerProvider : GradleTaskCheckerProvider() {

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
    return KtNewGradleTaskChecker(task, envChecker, project)
  }

  override fun mainClassForFile(project: Project, file: VirtualFile): String? {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null
    val mainFunction = PsiTreeUtil.findChildrenOfType(psiFile, KtElement::class.java).find { it.isMainFunction() } ?: return null
    val container = KotlinRunConfigurationProducer.getEntryPointContainer(mainFunction) ?: return null
    return KotlinRunConfigurationProducer.getStartClassFqName(container)
  }
}
