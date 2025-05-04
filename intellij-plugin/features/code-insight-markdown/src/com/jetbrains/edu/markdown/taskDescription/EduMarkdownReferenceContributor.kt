package com.jetbrains.edu.markdown.taskDescription

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.edu.codeInsight.EduReferenceContributorBase
import com.jetbrains.edu.codeInsight.taskDescription.InCourseLinkReferenceProviderBase
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownLinkDestination

class EduMarkdownReferenceContributor : EduReferenceContributorBase() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerEduReferenceProvider(MarkdownInCourseLinkReferenceProvider())
  }
}

private class MarkdownInCourseLinkReferenceProvider : InCourseLinkReferenceProviderBase() {
  override val pattern: ElementPattern<out PsiElement>
    get() = EduMarkdownPsiPatterns.markdownLinkDestination
  override val PsiElement.textWithOffset: TextWithOffset?
    get() {
      val linkDestination = this as? MarkdownLinkDestination ?: return null
      return TextWithOffset(linkDestination.text, 0)
    }

  override fun createFileReferenceSet(path: String, element: PsiElement, valueOffset: Int): CourseFileReferenceSet {
    return MarkdownCourseFileReferenceSet(path, element, valueOffset)
  }

  private inner class MarkdownCourseFileReferenceSet(
    path: String,
    element: PsiElement,
    valueOffset: Int
  ) : CourseFileReferenceSet(path, element, valueOffset) {

    override fun createFileReference(baseFileReference: FileReference): InCourseLinkReference {
      return MarkdownInCourseLinkReference(baseFileReference)
    }
  }

  private class MarkdownInCourseLinkReference(reference: FileReference) : InCourseLinkReference(reference) {
    override fun rename(newName: String): PsiElement {
      require(element is MarkdownLinkDestination)
      val children = generateSequence(element.firstChild, PsiElement::getNextSibling).toList()
      // Markdown plugin parses course link as `course`, `:`, `//path` tokens instead of single one.
      // So we have to handle rename manually since base implementation expects single leaf child in MarkdownLinkDestinationImpl
      // and fails because of wrong ranges
      val range = TextRange(fileReferenceSet.startInElement, rangeInElement.endOffset)
      for (child in children) {
        if (child !is LeafPsiElement) continue
        val childRange = child.textRangeInParent
        if (range in childRange) {
          val newText = range.shiftLeft(childRange.startOffset).replace(child.text, newName)
          child.replaceWithText(newText)
          break
        }
      }
      return element
    }
  }
}
