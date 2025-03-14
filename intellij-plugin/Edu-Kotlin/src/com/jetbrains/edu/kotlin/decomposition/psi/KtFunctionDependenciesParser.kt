package com.jetbrains.edu.kotlin.decomposition.psi

import com.intellij.psi.PsiFile
import com.jetbrains.edu.decomposition.parsers.FunctionDependenciesParser
import org.jetbrains.kotlin.psi.KtCallExpression

class KtFunctionDependenciesParser : FunctionDependenciesParser {
  override fun extractFunctionDependencies(files: List<PsiFile>): Map<String, List<String>> {
    val functions = getFunctionsPsi(files)
    val regex = """::\s*`([^`]+)\s*`|::\s*(\w+)\s*""".toRegex()
    return functions.mapNotNull { function ->
      val name = function.name ?: return@mapNotNull null

      val dependencies = function.bodyBlockExpression
       ?.statements
       ?.filterIsInstance<KtCallExpression>()
       ?.filter { it.calleeExpression?.text == "dependsOn" }
       ?.mapNotNull { callExpression ->
         val extractedDeps = callExpression.valueArguments.mapNotNull { arg ->
           regex.matchEntire(arg.text)?.let { match ->
             match.groups[1]?.value ?: match.groups[2]?.value
           }
         }
         if (extractedDeps.size != callExpression.valueArguments.size) null else extractedDeps
       }
       ?.flatten()
     ?: emptyList()

      name to dependencies
    }.groupBy({ it.first }, { it.second }).mapValues { entry -> entry.value.flatten() }
  }
}
