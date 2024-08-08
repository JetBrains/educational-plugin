package com.jetbrains.edu.markdown.taskDescription

import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.jetbrains.edu.codeInsight.inCourse
import com.jetbrains.edu.codeInsight.inFileWithName
import com.jetbrains.edu.codeInsight.psiElement
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.taskToolWindow.links.TaskDescriptionLinkProtocol
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownLinkDestination

object EduMarkdownPsiPatterns {

  val markdownLinkDestination: PsiElementPattern.Capture<out PsiElement> = psiElement<MarkdownLinkDestination>()
      .inCourse()
      .inFileWithName(DescriptionFormat.MD.fileName)

  val inMarkdownLinkDestination: PsiElementPattern.Capture<PsiElement> = psiElement<PsiElement>()
      .withParent(markdownLinkDestination)

  val toolWindowIdUriPath: PsiElementPattern.Capture<PsiElement> = uriPathElement(TaskDescriptionLinkProtocol.TOOL_WINDOW)

  val settingsIdUriPath: PsiElementPattern.Capture<PsiElement> = uriPathElement(TaskDescriptionLinkProtocol.SETTINGS)

  private fun uriPathElement(protocol: TaskDescriptionLinkProtocol): PsiElementPattern.Capture<PsiElement> {
    return psiElement<PsiElement>()
      .withParent(markdownLinkDestination)
      .afterLeaf(psiElement<PsiElement>().withText(":").afterLeaf(protocol.protocol.substringBefore(":")))
      .withText(StandardPatterns.string().startsWith("//"))
  }
}
