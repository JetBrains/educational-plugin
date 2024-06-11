package com.jetbrains.edu.cpp.checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childLeafs
import com.jetbrains.cidr.cpp.runfile.CppFileEntryPointDetector

// BACKCOMPAT: 2023.3. Convert into private method of `CppCodeExecutor`
fun findEntryPointElement(project: Project, virtualFile: VirtualFile): PsiElement? {
  val psiFile = virtualFile.findPsiFile(project) ?: return null
  val entryPointDetector = CppFileEntryPointDetector.getInstance() ?: return null
  // TODO: is there more efficient way to do it than iterating over all leaf children without referring to particular psi classes?
  // It still doesn't work with new C++ language engine because
  // `CppFileNovaEntryPointDetector#isMainOrIsInMain` is not properly implemented yet.
  // See https://youtrack.jetbrains.com/issue/EDU-6773
  return psiFile.childLeafs().find { entryPointDetector.isMainOrIsInMain(it) }
}
