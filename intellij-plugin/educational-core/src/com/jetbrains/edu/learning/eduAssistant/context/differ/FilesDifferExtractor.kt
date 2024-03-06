package com.jetbrains.edu.learning.eduAssistant.context.differ

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.eduAssistant.SignatureSource
import com.jetbrains.edu.learning.courseFormat.ext.getSolution
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.FunctionSignatureResolver
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.FunctionSignaturesProvider
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.createPsiFileForSolution
import com.jetbrains.edu.learning.navigation.NavigationUtils

fun getChangedContent(task: Task, taskFile: TaskFile, project: Project): String? {
  val virtualFile = taskFile.getVirtualFile(project) ?: return null
  val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return null
  return psiFile.filterAllowedModifications(task, taskFile, project, SignatureSource.VISIBLE_FILE)
}

private fun findChangedFunctions(task: Task, taskFile: TaskFile, project: Project) {
  if (task.changedFunctions == null) {
    val language = task.course.languageById ?: return
    val previousTask = NavigationUtils.previousTask(task)
    val previousTaskFile = previousTask?.taskFiles?.get(taskFile.name)
    previousTaskFile?.let {
      val beforePsiFile = it.getSolution().createPsiFileForSolution(project, language)
      val afterPsiFile = taskFile.getSolution().createPsiFileForSolution(project, language)
      val changedFunctions = FilesDiffer.findDifferentMethods(beforePsiFile, afterPsiFile, language)
      task.changedFunctions = changedFunctions
    }
  }
}

/**
 * Filters the allowed changed functions in the psi file at the current stage:
 * which are either contained in task.changedFunctions (updated functions in the authoring solution in comparison with the previous step)
 * or not contained in the author's solution.
 */
fun PsiFile.filterAllowedModifications(task: Task, taskFile: TaskFile, project: Project, signatureSource: SignatureSource): String {
  findChangedFunctions(task, taskFile, project)
  val language = task.course.languageById ?: return ""
  return runReadAction {
    FunctionSignaturesProvider.getFunctionSignatures(this, signatureSource, language).filter { functionSignature ->
      task.changedFunctions?.contains(functionSignature.name) == true ||
      task.authorSolutionContext?.functionSignatures?.contains(functionSignature) == false
    }.joinToString(separator = System.lineSeparator()) {
      FunctionSignatureResolver.getFunctionBySignature(this, it.name, language)?.text ?: ""
    }
  }
}
