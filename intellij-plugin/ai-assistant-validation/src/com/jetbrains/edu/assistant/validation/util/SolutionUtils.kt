package com.jetbrains.edu.assistant.validation.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.ext.getSolution
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.context.differ.FilesDiffer

const val TARGET_FILE_NAME_FOR_SOLUTIONS = "Main.kt"

// Propagate the author solution from the previous step to be able to update implemented functions for prompts
fun propagateAuthorSolution(previousTask: Task, currentTask: Task, project: Project) {
  replaceTaskFilesWithSolutions(currentTask, project) { fileName ->
    previousTask.taskFiles[fileName]?.getSolution()
  }
}

fun downloadSolution(task: Task, project: Project, studentCode: String) {
  replaceTaskFilesWithSolutions(task, project) { fileName ->
    if (fileName.endsWith(TARGET_FILE_NAME_FOR_SOLUTIONS)) {
      studentCode
    } else {
      null
    }
  }
}

fun replaceTaskFilesWithSolutions(task: Task, project: Project, solutionProducer: (String) -> (String?)) {
  task.taskFiles.filter { !it.value.isTestFile && it.value.isVisible }.forEach { (k, f) ->
    solutionProducer(k)?.let { solution ->
      replaceDocumentText(f, project, solution)
    }
  }
}

private fun replaceDocumentText(taskFile: TaskFile, project: Project, solution: String) {
  val currentDocument = taskFile.getDocument(project)
  ApplicationManager.getApplication().invokeAndWait {
    ApplicationManager.getApplication().runWriteAction {
      currentDocument?.setText(solution)
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
