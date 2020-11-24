package com.jetbrains.edu.markdown.taskDescription

import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeInsight.inCourse
import com.jetbrains.edu.learning.codeInsight.inFileWithName
import com.jetbrains.edu.learning.codeInsight.psiElement
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownLinkDestinationImpl

object EduMarkdownPsiPatterns {

  val inMarkdownLinkDestination: PsiElementPattern.Capture<PsiElement>
    get() = psiElement<PsiElement>()
      .inCourse()
      .inFileWithName(EduNames.TASK_MD)
      .withParent(psiElement<MarkdownLinkDestinationImpl>())
}
