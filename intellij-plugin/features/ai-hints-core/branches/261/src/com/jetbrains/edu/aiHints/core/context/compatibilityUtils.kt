package com.jetbrains.edu.aiHints.core.context

import com.intellij.lang.Language
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getSolution

suspend fun createFunctionsToString(project: Project, language: Language, files: List<TaskFile>): FunctionsToStrings =
  smartReadAction(project) {
    val course = project.course ?: error("Course is null for project $project")
    val functionsToStrings = files.asSequence()
      .flatMap { file ->
        val psiFileSolution = PsiFileFactory.getInstance(project).createFileFromText("psiFile", language, file.getSolution())
        val functionsToStringsMap = EduAIHintsProcessor.forCourse(course)
                                      ?.getStringsExtractor()
                                      ?.getFunctionsToStringsMap(psiFileSolution) ?: return@flatMap emptySequence()
        functionsToStringsMap.value.entries.asSequence()
      }
      .associate { it.toPair() }
    return@smartReadAction FunctionsToStrings(functionsToStrings)
  }