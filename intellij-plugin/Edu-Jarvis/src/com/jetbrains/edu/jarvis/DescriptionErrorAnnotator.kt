package com.jetbrains.edu.jarvis

import com.intellij.lang.annotation.Annotator

/**
 * Highlights parts containing errors inside the `description` DSL element.
 */

interface DescriptionErrorAnnotator : Annotator {

  fun getIncorrectParts(descriptionText: String): Sequence<DescriptionAnnotatorResult>

  companion object {
    val codeBlockRegex = "`([^`]+)`".toRegex()
  }

}
