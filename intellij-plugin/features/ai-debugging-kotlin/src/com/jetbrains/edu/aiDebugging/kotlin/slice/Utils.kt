package com.jetbrains.edu.aiDebugging.kotlin.slice

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtReturnExpression

private val reflectedAssignmentOperators = listOf("+=", "*=", "-=", "/=")
private val assignmentOperators = listOf("=") + reflectedAssignmentOperators

/**
 * Adds a dependency mapping from the `from` element to the `to` element in the current `PsiElementToDependencies`
 * map, ensuring that the dependency is added only if it does not already exist.
 *
 * Additionally, if the `outerScopes` parameter is provided, the same dependency is added to each of the
 * outer scope dependencies recursively.
 *
 * If `to` is `null` `from` just added as a key for further use.
 *
 * @param from The source `PsiElement` representing the key in the dependencies map.
 * @param to The target `PsiElement` to be added as a dependency for the `from` element. Can be `null`.
 * @param parentScopes A collection of other `PsiElementToDependencies` maps representing outer scopes where
 * the dependency should also be added. Defaults to `null`.
 *
 * If to is `null` it means that empty set will be defined.
 * This is done for store already initialized variables for making references on they in the future.
 */
fun MutablePsiElementToDependencies.addIfAbsent(
  from: PsiElement,
  to: PsiElement? = null,
  parentScopes: Collection<MutablePsiElementToDependencies>? = null
) {
  getOrPut(from) { hashSetOf() }.also { collection ->
    to?.let {
      collection.add(it)
    }
  }
  parentScopes?.forEach {
    it.addIfAbsent(from, to)
  }
}

fun PsiElement.references(): List<KtReferenceExpression> {
  val references = PsiTreeUtil.findChildrenOfType(this, KtReferenceExpression::class.java)
    .filter { it !is KtOperationReferenceExpression }
  return if (this is KtReferenceExpression) {
    references + this
  } else {
    references
  }
}

fun KtSingleValueToken?.isAssigment() = this != null && value in assignmentOperators

fun PsiElementToDependencies.copy() = mapValues { it.value.toHashSet() }.toMutableMap()

inline fun Sequence<PsiElement>.forEachReachable(action: (PsiElement) -> Unit) = iterator().forEachReachable(action)

inline fun Array<PsiElement>.forEachReachable(action: (PsiElement) -> Unit) = iterator().forEachReachable(action)

inline fun Iterator<PsiElement>.forEachReachable(action: (PsiElement) -> Unit) = forEach {
  if (it is KtReturnExpression || it is KtContinueExpression || it is KtBreakExpression) {
    return
  }
  action(it)
}
