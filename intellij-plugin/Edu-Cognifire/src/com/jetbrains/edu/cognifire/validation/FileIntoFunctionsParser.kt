package com.jetbrains.edu.cognifire.validation

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Interface for parsing function signatures and their corresponding bodies from a task.
 * It is used for validation.
 */
interface FileIntoFunctionsParser {
  fun parseFunctionSignaturesAndBodies(task: Task): Map<FunctionSignature, String>

  companion object {
    private val EP_NAME = LanguageExtension<FileIntoFunctionsParser>("Educational.fileIntoFunctionsParser")

    fun parseFunctionSignaturesAndBodies(task: Task, language: Language): Map<FunctionSignature, String> {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language)?.parseFunctionSignaturesAndBodies(task) ?: emptyMap()
    }
  }
}
