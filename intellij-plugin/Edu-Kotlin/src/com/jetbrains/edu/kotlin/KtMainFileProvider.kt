package com.jetbrains.edu.kotlin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.jetbrains.edu.jvm.MainFileProvider
import org.jetbrains.kotlin.idea.base.codeInsight.KotlinMainFunctionDetector
import org.jetbrains.kotlin.idea.base.codeInsight.findMainOwner
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer.Companion.getMainClassJvmName
import org.jetbrains.kotlin.psi.KtFile

class KtMainFileProvider : MainFileProvider {
  override fun findMainClassName(project: Project, file: VirtualFile): String? {
    val psiFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return null
    val container = KotlinMainFunctionDetector.getInstanceDumbAware(project).findMainOwner(psiFile) ?: return null
    return getMainClassJvmName(container)
  }

  override fun findMainPsi(project: Project, file: VirtualFile): PsiElement? {
    // It's not used now at all
    // TODO: refactor `MainFileProvider` and drop `findMainPsi`
    return null
  }
}
