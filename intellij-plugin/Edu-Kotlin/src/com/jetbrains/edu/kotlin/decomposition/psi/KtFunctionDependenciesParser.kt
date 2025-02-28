package com.jetbrains.edu.kotlin.decomposition.psi

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.decomposition.parsers.FunctionDependenciesParser
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

class KtFunctionDependenciesParser : FunctionDependenciesParser {
  override fun extractFunctionDependencies(files: List<PsiFile>): Map<String, List<String>> {
    val dependencyGraph = mutableMapOf<String, MutableSet<String>>()

    val functions = files.flatMap { PsiTreeUtil.findChildrenOfType(it, KtNamedFunction::class.java) }

    for (function in functions) {
      val name = function.name ?: continue

      val dependencies = function.bodyBlockExpression?.let { body ->
        body.statements
          .filterIsInstance<KtCallExpression>()
          .filter { it.calleeExpression?.text == "dependsOn" }
          .flatMap { it.valueArguments.mapNotNull { arg -> arg.text.drop(3).dropLast(1) } }
      } ?: emptyList()

      dependencyGraph.computeIfAbsent(name) { mutableSetOf() }.addAll(dependencies)
    }

    return dependencyGraph.mapValues { it.value.toList() }
  }
}
