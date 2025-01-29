package com.jetbrains.edu.aiHints.python.impl

import com.intellij.psi.PsiFile
import com.jetbrains.edu.aiHints.core.api.FilesDiffer
import com.jetbrains.edu.aiHints.python.impl.PyHintsUtils.functions
import com.jetbrains.edu.aiHints.python.impl.PyHintsUtils.hasSameBodyAs
import com.jetbrains.edu.aiHints.python.impl.PyHintsUtils.hasSameParametersAs
import com.jetbrains.python.psi.PyFunction

object PyFilesDiffer : FilesDiffer {
  override fun findChangedMethods(before: PsiFile, after: PsiFile, considerParameters: Boolean): List<String> {
    val beforeFunctions = before.functions().associateBy { it.fqName }
    val afterFunctions = after.functions().associateBy { it.fqName }

    return afterFunctions.values.filterNot { function ->
      // Include function in the result if there's no corresponding function from the original file
      val originalFunction = beforeFunctions[function.fqName] ?: return@filterNot false
      function hasSameBodyAs originalFunction && (!considerParameters || function hasSameParametersAs originalFunction)
    }.mapNotNull { it.name }
  }

  private val PyFunction.fqName: String
    get() = buildString {
      append(containingFile)
      append(":")
      if (containingClass != null) {
        append(containingClass)
        append(":")
      }
      append(name)
    }
}