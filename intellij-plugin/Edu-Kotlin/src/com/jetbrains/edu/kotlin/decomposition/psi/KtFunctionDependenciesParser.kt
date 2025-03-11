package com.jetbrains.edu.kotlin.decomposition.psi

import com.intellij.psi.PsiFile
import com.jetbrains.edu.decomposition.parsers.FunctionDependenciesParser
import org.jetbrains.kotlin.psi.KtCallExpression

class KtFunctionDependenciesParser : FunctionDependenciesParser {
  override fun extractFunctionDependencies(files: List<PsiFile>): Map<String, List<String>> {
    val functions = getFunctionsPsi(files)
    val regex = """::\s*`([^`]+)`|::\s*(\w+)""".toRegex()
    return functions.mapNotNull { function ->
      val name = function.name ?: return@mapNotNull null

      val dependencies = function.bodyBlockExpression?.let { body ->
        body.statements
          .filterIsInstance<KtCallExpression>()
          .filter { it.calleeExpression?.text == "dependsOn" }
          .flatMap { it.valueArguments.mapNotNull { arg ->
            regex.find(arg.text)?.let { match ->
              match.groups[1]?.value ?: match.groups[2]?.value
            } }
          }
      } ?: emptyList()

      name to dependencies
    }.groupBy({ it.first }, { it.second }).mapValues { entry -> entry.value.flatten() }
  }
}
