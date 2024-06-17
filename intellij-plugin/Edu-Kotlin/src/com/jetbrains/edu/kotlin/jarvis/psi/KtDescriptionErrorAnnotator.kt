package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.codeBlockRegex
import com.jetbrains.edu.jarvis.DescriptionAnnotatorResult
import com.jetbrains.edu.jarvis.errors.AnnotatorError
import com.jetbrains.edu.jarvis.messages.EduJarvisBundle
import com.jetbrains.edu.kotlin.jarvis.utils.isDescriptionBlock
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KtDescriptionErrorAnnotator : DescriptionErrorAnnotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (!element.containsCode()) return

    val descriptionContent = element.getChildOfType<KtValueArgumentList>() ?: return

    getIncorrectParts(descriptionContent.text).forEach {
      val errorRange = TextRange(
        descriptionContent.startOffset + it.range.first,
        descriptionContent.startOffset + it.range.last + 1
      )
      holder.newAnnotation(HighlightSeverity.ERROR, EduJarvisBundle.message(it.error.message))
        .range(errorRange)
        .create()
    }
  }

  override fun getIncorrectParts(descriptionText: String) =
    codeBlockRegex
      .findAll(descriptionText)
      .mapNotNull { it.groups[1] }
      .map {
        DescriptionAnnotatorResult(
          it.range,
          it.value.getError()
        )
      }
      .filter { it.error != AnnotatorError.NONE }

  private fun PsiElement.containsCode() = isDescriptionBlock()

  private fun String.getError(): AnnotatorError {
    // TODO: Actually detect errors
    return if (this.lowercase().contains("bad")) AnnotatorError.PLACEHOLDER_ERROR
    else AnnotatorError.NONE
  }
}