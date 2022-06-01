package com.jetbrains.edu.markdown.taskDescription

import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.jetbrains.edu.codeInsight.inCourse
import com.jetbrains.edu.codeInsight.inFileWithName
import com.jetbrains.edu.codeInsight.psiElement
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_MD
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownLinkDestinationImpl

object EduMarkdownPsiPatterns {

  val markdownLinkDestination: PsiElementPattern.Capture<out PsiElement> = psiElement<MarkdownLinkDestinationImpl>()
      .inCourse()
      .inFileWithName(TASK_MD)

  val inMarkdownLinkDestination: PsiElementPattern.Capture<PsiElement> = psiElement<PsiElement>()
      .withParent(markdownLinkDestination)
}
