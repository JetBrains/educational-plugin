package com.jetbrains.edu.cognifire

import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.AnnotatorError
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.AnnotatorParametrizedError
import com.jetbrains.edu.cognifire.models.NamedFunction
import com.jetbrains.edu.cognifire.models.NamedVariable

/**
 * Returns an [AnnotatorParametrizedError] associated with the relevant context.
 */
class ErrorProcessor(
  private val visibleFunctions: Collection<NamedFunction>, val visibleVariables: MutableCollection<NamedVariable>
) {

  /**
   * Processes the provided [NamedFunction].
   * Returns the associated [AnnotatorParametrizedError].
   */
  fun processNamedFunction(namedFunction: NamedFunction): AnnotatorParametrizedError {

    return when {
      visibleFunctions.none { it.name == namedFunction.name } -> AnnotatorParametrizedError(
        AnnotatorError.UNKNOWN_FUNCTION, arrayOf(namedFunction.name)
      )

      visibleFunctions.none { namedFunction.isCompatibleWith(it) } -> {
        AnnotatorParametrizedError(
          AnnotatorError.WRONG_NUMBER_OF_ARGUMENTS,
          arrayOf(namedFunction.name, namedFunction.numberOfArguments.first, namedFunction.argumentsToString())
        )
      }

      else -> AnnotatorParametrizedError.NO_ERROR

    }
  }

  /**
   * Processes the provided [NamedVariable].
   * Returns the associated [AnnotatorParametrizedError].
   */
  fun processNamedVariable(namedVariable: NamedVariable): AnnotatorParametrizedError {
    return if (namedVariable !in visibleVariables) {
      AnnotatorParametrizedError(
        AnnotatorError.UNKNOWN_VARIABLE, arrayOf(namedVariable.name)
      )
    }
    else AnnotatorParametrizedError.NO_ERROR
  }

  /**
   * Returns the associated [AnnotatorParametrizedError] if there is the [codePromptContent] and the [NamedVariable].
   */
  fun processVariableDeclaration(namedVariable: NamedVariable, codePromptContent: PsiElement?): AnnotatorParametrizedError {
    return if (codePromptContent != null) {
      AnnotatorParametrizedError(
        AnnotatorError.VARIABLE_DECLARATION, arrayOf(namedVariable.name)
      )
    }
    else AnnotatorParametrizedError.NO_ERROR
  }

  companion object {
    const val AND = "and"
    const val ARGUMENT_SEPARATOR = ','
  }

}