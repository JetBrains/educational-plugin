package com.jetbrains.edu.kotlin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.configuration.MainFileProvider
import org.jetbrains.kotlin.idea.isMainFunction
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer
import org.jetbrains.kotlin.psi.KtElement

class KtMainFileProvider : MainFileProvider {
  override fun findMainClass(project: Project, file: VirtualFile): String? {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null
    val mainFunction = PsiTreeUtil.findChildrenOfType(psiFile, KtElement::class.java).find { it.isMainFunction() } ?: return null
    val container = KotlinRunConfigurationProducer.getEntryPointContainer(mainFunction) ?: return null
    return KotlinRunConfigurationProducer.getStartClassFqName(container)
  }
}