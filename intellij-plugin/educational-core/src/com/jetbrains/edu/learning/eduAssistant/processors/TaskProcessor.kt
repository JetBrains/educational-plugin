package com.jetbrains.edu.learning.eduAssistant.processors

import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionSignature
import com.jetbrains.edu.learning.courseFormat.eduAssistant.SignatureSource
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.eduAssistant.context.StringExtractor
import com.jetbrains.edu.learning.eduAssistant.context.differ.filterAllowedModifications
import com.jetbrains.edu.learning.eduAssistant.context.differ.getChangedContent
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.*
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import org.jsoup.Jsoup

class TaskProcessor(val task: Task) {

  // Only for the Kotlin Onboarding Introduction: https://plugins.jetbrains.com/plugin/21067-kotlin-onboarding-introduction
  // and for Edu tasks
  fun isNextStepHintApplicable() = task.course.id == 21067 && task is EduTask

  fun isGetHintButtonShown() = isNextStepHintApplicable() && task.course.courseMode == CourseMode.STUDENT && task.status != CheckStatus.Solved // TODO: when should we show this button?

  fun taskHasErrors() = getFailureMessage() !== null && getFailedTestName() != null

  fun getFailureMessage() = if (task.status == CheckStatus.Failed) task.feedback?.message else null

  fun getFailedTestName() = if (task.status == CheckStatus.Failed) task.feedback?.failedTestInfo?.name else null

  fun getExpectedValue() = if (task.status == CheckStatus.Failed) task.feedback?.expected else null

  fun getActualValue() = if (task.status == CheckStatus.Failed) task.feedback?.actual else null

  private fun getTaskText(localTask: Task): String {
    return runReadAction { localTask.project?.let { localTask.getTaskTextFromTask(it) } ?: localTask.descriptionText }
  }

  private fun getTaskContentHtmlDocument() = Jsoup.parse(getTaskText(task).trimIndent())

  fun getTaskTextRepresentation(): String {
    val document = getTaskContentHtmlDocument()
    document.getElementsByClass("hint").remove()
    return document.text()
  }

  fun getHintsTextRepresentation(): List<String> {
    val document = getTaskContentHtmlDocument()
    return document.getElementsByClass("hint").map { it.text() }
  }

  fun getTheoryTextRepresentation(): String {
    val tasks = task.lesson.taskList
    return tasks.subList(0, tasks.indexOf(task)).filterIsInstance<TheoryTask>().joinToString(System.lineSeparator()) { it.presentableName }
  }

  fun getSubmissionTextRepresentation(state: EduState) = runReadAction {
    getChangedContent(task, state.taskFile, state.project)
  }

  fun getFunctionsFromTask(): List<FunctionSignature>? {
    val project = task.project ?: return null
    return task.taskFiles.values.filterNot { it.isTestFile }.flatMap { file ->
      getFunctionSignaturesIfFileUnchanged(file, project)?.let {
        return@flatMap it
      } ?: run {
        runReadAction { getFunctionSignatures(task, file, project) }.also {
          file.functionSignatures = it
        }
      }
    }
  }

  private fun getStringsIfFileUnchanged(file: TaskFile, project: Project): List<String>? = if (isFileUnchanged(
      file,
      project
    ) && file.usedStringsSnapshotHash == file.snapshotFileHash) file.usedStrings
  else null.also { file.usedStringsSnapshotHash = file.snapshotFileHash }

  fun getStringsFromTask(): List<String> {
    val project = task.project ?: return emptyList()
    val language = task.course.languageById ?: return emptyList()
    return task.taskFiles.values.filterNot { it.isTestFile }.flatMap { file ->
      getStringsIfFileUnchanged(file, project)?.let {
        return@flatMap it
      } ?: run {
        val virtualFile = file.getVirtualFile(project) ?: return@flatMap emptyList()
        runReadAction {
          val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@runReadAction emptyList()
          StringExtractor.getFunctionsToStringsMap(psiFile, language).values.flatten()
        }.also {
          file.usedStrings = it
        }
      }
    }
  }

  fun hasFilesChanged(): Boolean {
    val project = task.project ?: return false
    return task.taskFiles.values.any { !isFileUnchanged(it, project) }
  }

  fun getShortFunctionSignatureIfRecommended(code: String, project: Project, language: Language): FunctionSignature? {
    val functionSignatures = getFunctionSignaturesFromGeneratedCode(code, project, language)
    if (functionSignatures.size == 1) {
      val signature = functionSignatures.first()
      val functionsSignaturesFromSolution = task.authorSolutionContext?.functionSignatures?.filter {
          it.bodyLineCount != null && (it.bodyLineCount ?: Int.MAX_VALUE) <= MAX_BODY_LINES_IN_SHORT_FUNCTION
        } ?: return null
      return if (functionsSignaturesFromSolution.contains(signature)) signature else null
    }
    return null
  }

  fun extractRequiredFunctionsFromCodeHint(codeHint: String, taskFile: TaskFile): String {
    val project = task.project ?: return ""
    val language = task.course.languageById ?: return ""
    val codeHintPsiFile = runReadAction { PsiFileFactory.getInstance(project).createFileFromText("codeHintPsiFile", language, codeHint) }
    return codeHintPsiFile.filterAllowedModifications(task, taskFile, project, SignatureSource.GENERATED_SOLUTION)
  }

  fun applyCodeHint(codeHint: String, taskFile: TaskFile): String? {
    val project = task.project ?: return null
    val language = task.course.languageById ?: return null
    val virtualFile = taskFile.getVirtualFile(project) ?: return null
    val virtualFileText = runReadAction { virtualFile.getTextFromTaskTextFile() } ?: return null
    val psiFileCopy = runReadAction {
      PsiFileFactory.getInstance(project).createFileFromText(
        "copy${virtualFile.name}", language, virtualFileText
      )
    }
    var isFileModified = false
    val codeHintPsiFile = runReadAction { PsiFileFactory.getInstance(project).createFileFromText("codeHintPsiFile", language, codeHint) }
    val functionSignaturesFromCodeHint = runReadAction {
      FunctionSignaturesProvider.getFunctionSignatures(
        codeHintPsiFile, SignatureSource.GENERATED_SOLUTION, language
      )
    }

    for (newFunction in functionSignaturesFromCodeHint) {
      runReadAction { FunctionSignatureResolver.getFunctionBySignature(codeHintPsiFile, newFunction, language) }?.let { psiNewFunction ->
        WriteCommandAction.runWriteCommandAction(project, null, null, {
          FunctionSignatureResolver.getFunctionBySignature(psiFileCopy, newFunction, language)?.replace(psiNewFunction)?.let {
            isFileModified = true
          } ?: run {
            psiFileCopy.add(psiNewFunction)
            isFileModified = true
          }
        }, psiFileCopy)
      }
    }
    if (!isFileModified) return null
    return psiFileCopy.text
  }

  companion object {
    const val MAX_BODY_LINES_IN_SHORT_FUNCTION = 3
  }
}
