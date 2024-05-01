package com.jetbrains.edu.learning.eduAssistant.context

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.courseFormat.eduAssistant.AiAssistantState
import com.jetbrains.edu.learning.courseFormat.eduAssistant.AuthorSolutionContext
import com.jetbrains.edu.learning.courseFormat.ext.getSolution
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.createPsiFileForSolution
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor

private fun <K, V> flatten(list: List<Map<K, V>>): Map<K, V> = mutableMapOf<K, V>().apply {
  for (innerMap in list) putAll(innerMap)
}

fun Task.buildAuthorSolutionContext(): AuthorSolutionContext? {
  val project = this.project ?: return null
  val language = this.course.languageById ?: return null
  val functionsToStringMap = flatten(this.taskFiles.values.filterNot { it.isTestFile }.map { file ->
    val psiFileSolution = runReadAction { file.getSolution().createPsiFileForSolution(project, language) }
    runReadAction { StringExtractor.getFunctionsToStringsMap(psiFileSolution, language) }
  })
  return AuthorSolutionContext(
    functionsToStringMap = functionsToStringMap.mapKeys{ (k, _) -> k.toString() }.filter{ (k, _) -> k.isNotEmpty() },
    functionSignatures = functionsToStringMap.keys
  )
}

fun initAiHintContext(task: Task) {
  val taskProcessor = TaskProcessor(task)
  if (!taskProcessor.isNextStepHintApplicable()) {
    return
  }
  if (task.authorSolutionContext == null) {
    task.authorSolutionContext = task.buildAuthorSolutionContext()
  }
  val project = task.project ?: error("Project was not found")
  project.service<TaskBasedAssistant>().launchGetTaskAnalysis(taskProcessor).invokeOnCompletion {
    if (it == null) {
      task.aiAssistantState = AiAssistantState.ContextInitialized
    }
  }
}
