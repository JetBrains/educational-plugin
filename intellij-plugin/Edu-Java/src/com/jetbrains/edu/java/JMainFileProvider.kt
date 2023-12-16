package com.jetbrains.edu.java

import com.intellij.execution.JavaExecutionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiMethodUtil
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.jvm.MainFileProvider

class JMainFileProvider : MainFileProvider {
  override fun findMainClassName(project: Project, file: VirtualFile): String? {
    val psiElement = findMainPsi(project, file) ?: return null
    return JavaExecutionUtil.getRuntimeQualifiedName(psiElement)
  }

  override fun findMainPsi(project: Project, file: VirtualFile): PsiClass? {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null
    return PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java).find { psiClass ->
      PsiMethodUtil.MAIN_CLASS.value(psiClass) && PsiMethodUtil.hasMainMethod(psiClass)
    }
  }
}
