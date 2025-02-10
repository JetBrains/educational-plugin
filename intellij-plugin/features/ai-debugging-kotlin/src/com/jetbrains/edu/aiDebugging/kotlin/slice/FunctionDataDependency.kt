package com.jetbrains.edu.aiDebugging.kotlin.slice

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.completion.reference
import org.jetbrains.kotlin.idea.intentions.branches
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.blockExpressionsOrSingle

/**
 * Represents a data dependency analyzer for a Kotlin function, allowing the construction of data dependency graphs.
 *
 * This class computes forward and backward data-dependency relationships between various code elements
 * within the given Kotlin function (`KtFunction`).
 *
 * @constructor Initializes the dependency analysis for the provided function and computes
 * data dependencies for all referenced code elements.
 *
 * @param ktFunction kotlin function for which data dependencies are analyzed.
 *
 * @property dependenciesForward A mapping from each code element to the set of other code elements
 * to which its data dependencies are forwarded.
 *
 * @property dependenciesBackward A mapping from each code element to the set of other code elements
 * from which it receives data dependencies.
 */
class FunctionDataDependency(ktFunction: KtFunction) : FunctionDependency() {

  init {
    processReferences(ktFunction, mutableMapOf())
  }


  /**
   * Processes the references within the provided PsiElement, managing dependencies and scope definitions.
   *
   * @param psiElement The PSI element to be analyzed and processed for references.
   * @param definedVariables Mapping PSI elements to their defined references that change their value.
   * As an example each increment or adding for a variable is counted. It should be done for comprehension of data dependency.
   * @param parentScopesDefinitions A list of definitions of variables and references
   * from parent scopes to be considered during processing. Defaults to an empty list.
   * Needed to cover all nested control flow branches.
   * As an example each `if` branch.
   */
  private fun processReferences(
    psiElement: PsiElement,
    definedVariables: PsiElementToDependencies,
    parentScopesDefinitions: List<PsiElementToDependencies> = emptyList()
  ) {
    with(ScopeContext(definedVariables, parentScopesDefinitions)) {
      when (psiElement) {
        is KtFunction -> psiElement.process()

        is KtProperty -> psiElement.process()

        is KtBinaryExpression -> psiElement.process()

        is KtPostfixExpression -> psiElement.process()

        is KtForExpression -> psiElement.process()

        is KtWhileExpression -> psiElement.process()

        is KtCallExpression -> psiElement.process()

        is KtWhenExpression -> psiElement.process()

        // TODO add elif expression
        is KtIfExpression -> psiElement.process()
      }
    }
  }

  /**
   * A context to manage the scope and defined variables during current state of processing.
   */
  private inner class ScopeContext(
    val definedVariables: PsiElementToDependencies,
    val parentScopesDefinitions: List<PsiElementToDependencies>
  ) {
    fun KtFunction.process() {
      valueParameters.forEach {
        definedVariables.addIfAbsent(it, parentScopes = parentScopesDefinitions)
      }

      bodyExpression?.blockExpressionsOrSingle()?.forEach {
        processReferences(it, definedVariables, parentScopesDefinitions)
      }
    }

    /**
     * The method registers the property into the [definedVariables]
     * and adding dependencies of references in initializer
     * Example:
     * ```
     * var a = 1
     *
     * ```
     */
    fun KtProperty.process() {
      definedVariables.addIfAbsent(this, parentScopes = parentScopesDefinitions)
      initializer?.let {
        it.getVariableReferences().forEach { reference ->
          addReferences(reference, definedVariables)
        }
      }
    }

    /**
     * This function looks for the corresponding property on the left side
     * of the expression and also looks for all used references.
     * The right side looks for all used variables and defined references.
     * Dependencies are created on this basis.
     *
     * Do not use [addReferences] because exactly BinaryExpression should be added for dependencies
     * And it should not contain cyclic dependencies.
     *
     * Example:
     * ```
     * var a = 1
     * var b = 2
     * b += 3
     * var s = a + b
     * ```
     *- `var s = a + b` depends on:
     *     - `var a = 1`
     *     - `var b = 2`
     *     - `b += 3`
     *
     * - `b += 3` depends on:
     *     - `var b = 2`
     *
     */
    fun KtBinaryExpression.process() {
      if (operationReference.operationSignTokenType.isAssigment()) {
        left?.reference()?.resolve()?.let { referenceTo ->
          definedVariables[referenceTo]?.forEach {
            if (this != it) {
              addDependency(it)
            }
          }
          addDependency(referenceTo)
          definedVariables.addIfAbsent(referenceTo, this, parentScopesDefinitions)
        }
        right?.getVariableReferences()?.forEach {
          addReferences(it, definedVariables)
        }
      }
    }

    /**
     * This method resolves the reference from the unary expression base element and updates dependency mappings.
     *
     * Example:
     * ```
     * a++
     * ```
     *
     * - `a++` depends on:
     *     - `a: Int` (function parameter)
     *
     */
    fun KtPostfixExpression.process() {
      baseExpression?.reference()?.resolve()?.let {
        definedVariables.addIfAbsent(it, this, parentScopesDefinitions)
        addDependency(it)
      }
    }

    /**
     * This method performs the following tasks:
     * - Registers the loop parameter in the `definedVariables` map.
     * - Resolves variable references from the loop range and updates the dependency mappings.
     * - Iteratively processes child elements within the body for variable definitions and further scope references.
     *
     * Example:
     * ```
     * var n = readln().toInt()
     * var sum = 0
     * var prod = 1
     * for (i in 0..n) {
     *   sum += i
     *   prod *= i
     * }
     * ```
     *
     * - `for (i in 0..n)` depends on:
     *     - `var n = readln().toInt()`
     *
     * - `sum += i` depends on:
     *     - `var sum = 0`
     *     - `i`
     *
     * - `prod *= i` depends on:
     *     - `var prod = 1`
     *     - `i`
     */
    fun KtForExpression.process() {
      loopParameter?.let {
        definedVariables.addIfAbsent(it, parentScopes = parentScopesDefinitions)
      }
      loopRange?.getVariableReferences()?.forEach {
        addReferences(it, definedVariables)
      }
      body?.children?.forEach {
        definedVariables.searchElements(it)
      }
      body?.children?.forEach {
        processReferences(it, definedVariables, parentScopesDefinitions)
      }
    }

    /**
     * This method performs the following tasks:
     * - Iterates over the children of the `body`, utilizing `searchElements` to handle variable definitions
     *   in the current and outer scope contexts.
     * - Processes references retrieved from the `condition`, updating the dependency mappings via `addReferences`.
     * - Further processes each element of the `body` via `processReferences` to manage scoped dependencies
     *   and nested structures.
     *
     * Example:
     * ```
     *  var n = readln().toInt()
     *  var i = 1
     *  var sum = 0
     *  var prod = 1
     *  while (i <= n) {
     *    sum += i
     *    prod *= i
     *    i++
     * }
     * ```
     * - `while (i <= n)` depends on:
     *     - `var n = readln().toInt()`
     *     - `var i = 1`
     *     - `i++`
     *
     * - `sum += i` depends on:
     *     - `var sum = 0`
     *     - `var i = 1`
     *     - `i++`
     *
     * - `prod *= i` depends on:
     *     - `var prod = 1`
     *     - `var i = 1`
     *     - `i++`
     *
     * - `i++` depends on:
     *     - `var i = 1`
     *
     */
    fun KtWhileExpression.process() {
      body?.children?.forEach {
        definedVariables.searchElements(it)
      }

      condition?.getVariableReferences()?.forEach {
        addReferences(it, definedVariables)
      }
      body?.children?.forEach {
        processReferences(it, definedVariables, parentScopesDefinitions)
      }
    }


    /**
     * This method performs the following tasks:
     * - Iterates over the arguments in the `valueArgumentList` of the call expression.
     * - For each argument, extracts its expression and retrieves any variable references.
     * - Each identified reference is processed by invoking `addReferences`, adding their dependencies.
     *
     */
    fun KtCallExpression.process() {
      valueArgumentList?.arguments?.forEach {
        it.getArgumentExpression()?.getVariableReferences()?.forEach { reference ->
          addReferences(reference, definedVariables)
        }
      }
    }

    /**
     * This method performs the following operations:
     * - Checks if the `subjectExpression` is a `KtReferenceExpression` and
     *   calls `addReferences` to record its dependencies.
     * - If an entry's `expression` is a `KtBlockExpression`, processes each statement within the block.
     * - For other expression types, calls `processReferences` to resolve and update references,
     *     working within an updated snapshot of `definedVariables` and `outerScopeDefinitions`.
     *
     * Example:
     * ```
     * val a = 1
     * var s = 2
     * when (a) {
     *   1 -> println(1)
     *   2 -> {
     *     s += 5
     *     s += a
     *   }
     *   else -> s += a + a
     * }
     * ```
     * - `when (a)` depends on:
     *     - `val a = 1`
     *
     * - `s += 5` depends on:
     *     - `var s = 2`
     *
     * - `s += a` depends on:
     *     - `val a = 1`
     *     - `var s = 2`
     *     - `s += 5`
     *
     * - `s += a + a` depends on:
     *     - `var s = 2`
     *     - `val a = 1`
     *
     */
    fun KtWhenExpression.process() {
      subjectExpression
        ?.takeIf { it is KtReferenceExpression }
        ?.let { addReferences(it as KtReferenceExpression, definedVariables) }
      val stateSnapshot = definedVariables.copy() // need to be copied due to execution branches
      entries.forEach { entry ->
        val blockState = stateSnapshot.copy() // each branch should have same state as it was on when expression
        entry.expression?.blockExpressionsOrSingle()?.forEach { statement ->
          // use definedVariables for collecting all necessary definitions in the control branch for this scope
          processReferences(statement, blockState, parentScopesDefinitions + definedVariables)
        }
      }
    }

    /**
     * This method performs the following tasks:
     * - Retrieves and processes variable references found in the `condition` of the `KtIfExpression`.
     * - For each branch of the `KtIfExpression` iteratively processes child
     * elements of the branch using `processReferences` to manage variable
     * definitions and update dependency mappings within the branch scope.
     *
     * Example:
     * ```
     * var a = 1
     * var b = a * 2
     * if (b > 10) {
     *   a += 2
     * } else {
     *   b += a
     * }
     * ```
     * - `var b = a * 2` depends on:
     *     - `var a = 1`
     *
     * - `if (b > 10)` depends on:
     *     - `var b = a * 2`
     *
     * - `a += 2` depends on:
     *     - `var a = 1`
     *
     * - `b += a` depends on:
     *     - `var a = 1`
     *     - `var b = a * 2`
     *
     */
    fun KtIfExpression.process() {
      condition?.getVariableReferences()?.forEach {
        addReferences(it, definedVariables)
      }
      val stateSnapshot = definedVariables.copy() // need to be copied due to execution branches
      branches.forEach { branch ->
        val branchState = stateSnapshot.copy() // each branch should have same state as it was on if expression
        branch?.children?.forEach { element ->
          // use definedVariables for collecting all necessary definitions in the control branch for this scope
          processReferences(element, branchState, parentScopesDefinitions + definedVariables)
        }
      }
    }

    /**
     * Does not make dependencies, but only adds the use of variables and references.
     * This is necessary because loops may not have linear dependencies
     */
    private fun PsiElementToDependencies.searchElements(psiElement: PsiElement) {
      when (psiElement) {
        is KtBinaryExpression -> {
          if (psiElement.operationReference.operationSignTokenType.isAssigment()) {
            psiElement.left?.reference()?.resolve()?.let {
              addIfAbsent(it, psiElement, parentScopesDefinitions)
            }
          }
        }

        is KtPostfixExpression -> {
          psiElement.baseExpression?.reference()?.resolve()?.let {
            addIfAbsent(it, psiElement, parentScopesDefinitions)
          }
        }
      }
    }
  }

  /**
   * Makes a dependency on the element and all of its already initialized references
   */
  private fun PsiElement.addReferences(other: KtReferenceExpression, definedVariables: PsiElementToDependencies) {
    other.reference()?.resolve()?.let { referenceTo ->
      addDependency(referenceTo)
      definedVariables[referenceTo]?.forEach {
        addDependency(it)
      }
    }
  }


  override fun PsiElement.addDependency(other: PsiElement) {
    dependenciesBackward.addIfAbsent(this, other)
    dependenciesForward.addIfAbsent(other, this)
  }
}
