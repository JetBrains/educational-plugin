package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.jarvis.DescriptionAnnotatorResult
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator
import com.jetbrains.edu.jarvis.enums.AnnotatorRule
import com.jetbrains.edu.jarvis.enums.AnnotatorError
import com.jetbrains.edu.jarvis.errors.AnnotatorParametrizedError
import com.jetbrains.edu.jarvis.models.NamedFunction
import com.jetbrains.edu.jarvis.models.NamedVariable
import com.jetbrains.edu.kotlin.KtErrorProcessor
import com.jetbrains.edu.kotlin.jarvis.utils.isDescriptionBlock
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class KtDescriptionErrorAnnotator : DescriptionErrorAnnotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (!element.isRelevant()) return
    val descriptionContent = element.getChildOfType<KtValueArgumentList>() ?: return
    applyAnnotation(descriptionContent, holder)
  }

  override fun PsiElement.isRelevant() = isDescriptionBlock()

  override fun getIncorrectParts(context: PsiElement): Collection<DescriptionAnnotatorResult> {
    val visibleFunctions = getVisibleFunctions(context)
    val visibleVariables = getVisibleVariables(context)
    return AnnotatorRule.values().asSequence()
      .flatMap { rule ->
        getIncorrectPartsByRegex(context, rule.regex).map {
          it to rule
        }
      }.sortedBy { (match, _) ->
        match.range.first
      }.distinctBy { (match, _) ->
        match.range.first
      }.map { (match, rule) ->
        DescriptionAnnotatorResult(
          match.range, match.value.getError(rule, visibleFunctions, visibleVariables)
        )
      }.filter { it.parametrizedError.errorType != AnnotatorError.NONE }
      .toList()

  }

  private fun getVisibleVariables(context: PsiElement) =
    (PsiTreeUtil.collectElementsOfType(context.containingFile, KtProperty::class.java)
     + PsiTreeUtil.collectElementsOfType(context.containingFile, KtParameter::class.java))
      .mapNotNull { it.toNamedVariableOrNull() }.toMutableSet()

  private fun getVisibleFunctions(context: PsiElement) =
    PsiTreeUtil.collectElementsOfType(context.containingFile, KtNamedFunction::class.java)
      .mapNotNull { it.toNamedFunctionOrNull() }.toMutableSet()

  private fun getIncorrectPartsByRegex(context: PsiElement, regex: Regex): Sequence<MatchGroup> {
    return regex.findAll(context.text).mapNotNull { it.groups[1] }
  }

  private fun PsiElement.toNamedFunctionOrNull(): NamedFunction? {
    if (this !is KtNamedFunction) return null
    val functionName = name ?: return null
    val numberOfParameters = getChildOfType<KtParameterList>()?.parameters?.size ?: 0
    return NamedFunction(functionName, numberOfParameters)
  }

  private fun PsiElement.toNamedVariableOrNull(): NamedVariable? {
    return when (this) {
      is KtProperty -> {
        val variableName = name ?: return null
        NamedVariable(variableName)
      }
      is KtParameter -> {
        val variableName = name ?: return null
        NamedVariable(variableName)
      }
      else -> null
    }
  }


  private fun String.isNamedFunction() = DescriptionErrorAnnotator.namedFunctionRegex.matches(this)

  private fun String.isNamedVariable() = DescriptionErrorAnnotator.namedVariableRegex.matches(this)

  private fun String.getError(
    rule: AnnotatorRule, visibleFunctions: Collection<NamedFunction>, visibleVariables: MutableSet<NamedVariable>
  ): AnnotatorParametrizedError {
    val processor = KtErrorProcessor(this, visibleFunctions, visibleVariables)
    return when (rule) {
      AnnotatorRule.VARIABLE_DECLARATION -> {
        visibleVariables.add(NamedVariable(this))
        AnnotatorParametrizedError.NO_ERROR
      }

      AnnotatorRule.NO_PARENTHESES_FUNCTION -> {
        processor.processNamedFunction()
      }

      AnnotatorRule.ISOLATED_CODE -> {
        when {
          isNamedFunction() -> processor.processNamedFunction()
          isNamedVariable() -> processor.processNamedVariable()
          else -> AnnotatorParametrizedError.NO_ERROR
        }
      }

      else -> AnnotatorParametrizedError.NO_ERROR
    }
  }
}
