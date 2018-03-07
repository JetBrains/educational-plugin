package com.jetbrains.edu.kotlin.checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.checker.gradle.GradleTaskCheckerProvider
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer
import org.jetbrains.kotlin.psi.KtElement

class KtTaskCheckerProvider : GradleTaskCheckerProvider() {

  override fun mainClassForFile(project: Project, file: VirtualFile): String? {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null
    val ktElements = PsiTreeUtil.findChildrenOfType(psiFile, KtElement::class.java)
    val container = KotlinRunConfigurationProducer.getEntryPointContainer(ktElements.first())
    if (container == null) {
      println("container is null")
    }
    val qualifiedName = KotlinRunConfigurationProducer.getStartClassFqName(container)
    if (qualifiedName == null) {
      println("Qualified name is null for $container")
    }
    return qualifiedName
  }
}
