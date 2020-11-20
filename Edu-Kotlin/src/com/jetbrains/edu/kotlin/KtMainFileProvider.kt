package com.jetbrains.edu.kotlin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.jvm.MainFileProvider
import org.jetbrains.kotlin.idea.isMainFunction
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer
import org.jetbrains.kotlin.psi.KtElement

class KtMainFileProvider : MainFileProvider {
  override fun findMainClassName(project: Project, file: VirtualFile): String? {
    val mainFunction = findMainPsi(project, file) ?: return null
    val container = KotlinRunConfigurationProducer.getEntryPointContainer(mainFunction) ?: return null
    return KotlinRunConfigurationProducer.getStartClassFqName(container)
  }

  override fun findMainPsi(project: Project, file: VirtualFile): PsiElement? {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null
    return PsiTreeUtil.findChildrenOfType(psiFile, KtElement::class.java).find { it.isMainFunction() }
  }
}
