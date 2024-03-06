package com.jetbrains.edu.assistant.validation.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.ext.getSolution
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.context.differ.FilesDiffer

// Propagate the author solution from the previous step to be able to update implemented functions for prompts
fun propagateAuthorSolution(previousTask: Task, currentTask: Task, project: Project) {
  val previousTaskFiles = previousTask.taskFiles
  currentTask.taskFiles.forEach { (k, f) ->
    if (!f.isTestFile && f.isVisible) {
      previousTaskFiles[k]?.getSolution()?.let { authorSolution ->
        val currentDocument = f.getDocument(project)
        ApplicationManager.getApplication().invokeAndWait{
          ApplicationManager.getApplication().runWriteAction {
            currentDocument?.setText(authorSolution)
          }
        }
      }
    }
  }
}

fun getAuthorSolution(task: EduTask, project: Project) =
  task.taskFiles.map { (_, taskFile) ->
    val solution = taskFile.getSolution()
    val language = project.course?.languageById ?: return@map ""
    val document = taskFile.getDocument(project) ?: return@map ""
    val solutionPsiFile = runReadAction { PsiFileFactory.getInstance(project).createFileFromText("solution", language, solution) }
    val psiFile = runReadAction { PsiFileFactory.getInstance(project).createFileFromText("currentUserFile", language, document.text) }
    runReadAction { FilesDiffer.findDifferentMethods(psiFile, solutionPsiFile, taskFile, language) }
  }.joinToString(System.lineSeparator())
