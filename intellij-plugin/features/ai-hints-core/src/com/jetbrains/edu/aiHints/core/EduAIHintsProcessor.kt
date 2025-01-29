package com.jetbrains.edu.aiHints.core

import com.intellij.lang.LanguageExtension
import com.jetbrains.edu.aiHints.core.api.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.languageById

/**
 * The main interface provides AI Hints support for some language.
 *
 * To get provider instance for the specific course use [EduAIHintsProcessor.forCourse].
 *
 * @see com.jetbrains.edu.aiHints.core.action.GetHint
 * @see com.jetbrains.edu.aiHints.kotlin.KtEduAiHintsProcessor
 * @see com.jetbrains.edu.aiHints.python.PyEduAiHintsProcessor
 */
interface EduAIHintsProcessor {
  fun getFilesDiffer(): FilesDiffer

  fun getFunctionDiffReducer(): FunctionDiffReducer

  fun getInspectionsProvider(): InspectionsProvider

  fun getFunctionSignatureManager(): FunctionSignaturesManager

  fun getStringsExtractor(): StringExtractor

  companion object {
    private val EP_NAME = LanguageExtension<EduAIHintsProcessor>("Educational.aiHintsProcessor")

    fun forCourse(course: Course): EduAIHintsProcessor? {
      return course.languageById?.let { EP_NAME.forLanguage(it) }
    }
  }
}