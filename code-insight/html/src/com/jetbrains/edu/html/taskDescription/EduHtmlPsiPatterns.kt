package com.jetbrains.edu.html.taskDescription

import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.XmlAttributeValuePattern
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTokenType
import com.jetbrains.edu.codeInsight.inCourse
import com.jetbrains.edu.codeInsight.inFileWithName
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_HTML
import com.jetbrains.edu.learning.taskDescription.A_TAG
import com.jetbrains.edu.learning.taskDescription.HREF_ATTRIBUTE

object EduHtmlPsiPatterns {

  val hrefAttributeValue: XmlAttributeValuePattern = XmlPatterns.xmlAttributeValue(HREF_ATTRIBUTE)
    .inCourse()
    .inFileWithName(TASK_HTML)
    .withSuperParent(2, XmlPatterns.xmlTag().withName(A_TAG))

  val inHrefAttributeValue: PsiElementPattern.Capture<PsiElement> = PlatformPatterns.psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)
    .withParent(hrefAttributeValue)
}
