package com.jetbrains.edu.cpp.checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import com.jetbrains.cidr.execution.OCTargetConfigurationHelper.isInEntryPointBody
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration

fun findEntryPointElement(project: Project, virtualFile: VirtualFile): PsiElement? {
  val psiFile = virtualFile.findPsiFile(project) ?: return null
  val functions = psiFile.childrenOfType<OCFunctionDeclaration>()
  return functions.find { isInEntryPointBody(it) }
}
