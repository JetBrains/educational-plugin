package com.jetbrains.edu.aiHints.core.context

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.learning.course
import com.intellij.lang.Language
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getSolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.toPair
import kotlin.sequences.flatMap

suspend fun createFunctionsToString(project: Project, language: Language, files: List<TaskFile>): FunctionsToStrings {
  val course = project.course ?: error("Course is null for project $project")
  val extractor = EduAIHintsProcessor.forCourse(course)?.getStringsExtractor() ?: return FunctionsToStrings(emptyMap())

  val psiFiles = withContext(Dispatchers.EDT) {
    files.map { file ->
      PsiFileFactory.getInstance(project).createFileFromText(file.name, language, file.getSolution())
    }
  }
  val functionsToStrings = smartReadAction(project) {
    psiFiles.asSequence()
      .flatMap { psiFile: PsiFile ->
        extractor.getFunctionsToStringsMap(psiFile).value.entries.asSequence()
      }
      .associate { it.toPair() }
  }
  return FunctionsToStrings(functionsToStrings)
}