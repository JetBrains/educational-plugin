package com.jetbrains.edu.html.taskDescription

import com.intellij.patterns.*
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTokenType
import com.jetbrains.edu.codeInsight.inCourse
import com.jetbrains.edu.codeInsight.inFileWithName
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.taskToolWindow.A_TAG
import com.jetbrains.edu.learning.taskToolWindow.HREF_ATTRIBUTE
import com.jetbrains.edu.learning.taskToolWindow.links.TaskDescriptionLinkProtocol

object EduHtmlPsiPatterns {

  val hrefAttributeValue: XmlAttributeValuePattern = XmlPatterns.xmlAttributeValue(HREF_ATTRIBUTE)
    .inCourse()
    .inFileWithName(DescriptionFormat.HTML.fileName)
    .withSuperParent(2, XmlPatterns.xmlTag().withName(A_TAG))

  val inHrefAttributeValue: PsiElementPattern.Capture<PsiElement> = PlatformPatterns.psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)
    .withParent(hrefAttributeValue)

  val toolWindowIdUriPath: PsiElementPattern.Capture<PsiElement> = uriPathElement(TaskDescriptionLinkProtocol.TOOL_WINDOW)

  val settingsIdUriPath: PsiElementPattern.Capture<PsiElement> = uriPathElement(TaskDescriptionLinkProtocol.SETTINGS)

  private fun uriPathElement(protocol: TaskDescriptionLinkProtocol): PsiElementPattern.Capture<PsiElement> {
    return inHrefAttributeValue.withText(StandardPatterns.string().startsWith(protocol.protocol))
  }
}
