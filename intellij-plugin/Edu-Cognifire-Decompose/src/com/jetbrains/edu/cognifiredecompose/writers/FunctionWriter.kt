package com.jetbrains.edu.cognifiredecompose.writers

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.edu.cognifiredecompose.models.FunctionExpression

/**
 * Adds a `function` DSL block with an empty function to the code after the given [PsiElement].
 */
interface FunctionWriter<FunctionExpression> {
  fun addExpression(project: Project, element: PsiElement): FunctionExpression?

  companion object {
    private val EP_NAME = LanguageExtension<FunctionWriter<FunctionExpression>>("Educational.functionWriter")

    /**
     * Adds a new function expression to the function after the given [PsiElement].
     * @return the offset of the code inside the code block
     * @throws IllegalStateException if the language doesn't support providing a code expression
     */
    fun addFunction(project: Project, element: PsiElement, language: Language): FunctionExpression {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language)?.addExpression(project, element)
        ?: error("Not supported to provide a function expression for the ${language.displayName} language")
    }

  }
}
