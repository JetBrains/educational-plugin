package com.jetbrains.edu.kotlin.cognifire.validation

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.cognifire.validation.FileIntoFunctionsParser
import com.jetbrains.edu.kotlin.cognifire.utils.getFunctionSignature
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtNamedFunction

class KtFileIntoFunctionsParser : FileIntoFunctionsParser {
  override fun parseFunctionSignaturesAndBodies(task: Task): Map<FunctionSignature, String> {
    val content = task.taskFiles.values.filter { it.isVisible }.joinToString(System.lineSeparator()) { it.contents.textualRepresentation }
    val virtualFile = LightVirtualFile("Main.kt", KotlinLanguage.INSTANCE, content)
    val psiFile =  PsiFileFactory.getInstance(task.project).createFileFromText(
      virtualFile.name, virtualFile.fileType, virtualFile.content,  virtualFile.modificationStamp, false, true)
    val result = mutableMapOf<FunctionSignature, String>()
    psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
      override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        if (element is KtNamedFunction) {
          element.bodyExpression?.text?.let { body ->
            result[getFunctionSignature(element)] = body
          }
        }
      }
    })
    return result
  }
}
