package com.jetbrains.edu.html.taskDescription

import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.xml.XmlAttributeValue
import com.jetbrains.edu.codeInsight.EduReferenceContributorBase
import com.jetbrains.edu.codeInsight.taskDescription.InCourseLinkReferenceProviderBase

class EduHtmlReferenceContributor : EduReferenceContributorBase() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerEduReferenceProvider(HtmlInCourseLinkReferenceProvider())
  }
}

private class HtmlInCourseLinkReferenceProvider : InCourseLinkReferenceProviderBase() {
  override val pattern: ElementPattern<out PsiElement>
    get() = EduHtmlPsiPatterns.hrefAttributeValue
  override val PsiElement.textWithOffset: TextWithOffset?
    get() {
      val attributeValue = this as? XmlAttributeValue ?: return null
      val offset = attributeValue.valueTextRange.startOffset - attributeValue.textRange.startOffset
      return TextWithOffset(attributeValue.value, offset)
    }
}
