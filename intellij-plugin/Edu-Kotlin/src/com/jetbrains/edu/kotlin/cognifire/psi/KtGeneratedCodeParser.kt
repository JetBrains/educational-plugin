package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import com.jetbrains.edu.cognifire.parsers.GeneratedCodeParser
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.kotlin.cognifire.utils.createPsiFile
import com.jetbrains.edu.kotlin.cognifire.utils.getTodoMessageOrNull

class KtGeneratedCodeParser : GeneratedCodeParser {
  override fun hasErrors(project: Project, generatedCode: String, functionSignature: FunctionSignature): Boolean {
    val file = createPsiFile(project, functionSignature.toString(), generatedCode)
    return file.descendantsOfType<PsiElement>().any { it.getTodoMessageOrNull() != null }
  }
}
