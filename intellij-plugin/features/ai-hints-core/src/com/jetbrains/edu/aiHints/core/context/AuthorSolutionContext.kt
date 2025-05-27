package com.jetbrains.edu.aiHints.core.context

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.runReadActionInSmartMode

data class AuthorSolutionContext(
  val functionsToStringMap: Map<String, List<String>> = emptyMap(),
  val functionSignatures: Set<FunctionSignature> = emptySet()
) {
  fun isFunctionsPresented(): Boolean = functionSignatures.isNotEmpty()

  companion object {
    @JvmStatic
    fun create(project: Project, task: Task): AuthorSolutionContext {
      val language = task.course.languageById ?: error("Language is null for task ${task.name}")
      val files = task.taskFiles.values.filterNot { it.isTestFile }
      val functionsToStringMap = runReadActionInSmartMode(project) {
        FunctionsToStrings.create(project, language, files)
      }
      return AuthorSolutionContext(
        functionsToStringMap.mapOfStringSignatures(),
        functionsToStringMap.signatures()
      )
    }
  }
}