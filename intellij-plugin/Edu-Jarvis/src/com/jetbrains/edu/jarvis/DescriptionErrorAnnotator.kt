package com.jetbrains.edu.jarvis

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.edu.jarvis.errors.AnnotatorError
import com.jetbrains.edu.jarvis.messages.EduJarvisBundle

/**
 * Highlights parts containing errors inside the `description` DSL element.
 */

interface DescriptionErrorAnnotator : Annotator {

  fun applyAnnotation(
    descriptionContent: PsiElement,
    holder: AnnotationHolder
  ) =
    getIncorrectParts(descriptionContent.text).forEach {
      val errorRange = TextRange(
        descriptionContent.startOffset + it.range.first,
        descriptionContent.startOffset + it.range.last + 1
      )
      holder.newAnnotation(HighlightSeverity.ERROR, EduJarvisBundle.message(it.error.message))
        .range(errorRange)
        .create()
    }

  fun getIncorrectParts(descriptionText: String) =
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

  fun String.getError(): AnnotatorError

  fun PsiElement.isRelevant(): Boolean

  companion object {
    val codeBlockRegex = "`([^`]+)`".toRegex()
  }

}
