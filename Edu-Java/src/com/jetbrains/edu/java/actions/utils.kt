package com.jetbrains.edu.java.actions

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifier
import com.jetbrains.edu.learning.getTaskFile

fun adjustElement(element: PsiElement): PsiFile? {
  val psiClass = element as? PsiClass ?: return null
  if (psiClass.modifierList?.hasModifierProperty(PsiModifier.PUBLIC) != true) return null
  val containingFile = psiClass.containingFile
  val virtualFile = containingFile.virtualFile ?: return null
  val taskFile = virtualFile.getTaskFile(element.project) ?: return null
  return if (!taskFile.isLearnerCreated && virtualFile.nameWithoutExtension == psiClass.name) containingFile else null
}
