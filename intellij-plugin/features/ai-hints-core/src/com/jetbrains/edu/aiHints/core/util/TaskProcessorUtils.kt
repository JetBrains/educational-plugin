package com.jetbrains.edu.aiHints.core.util

import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.util.asSafely
import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.learning.course

fun String.createPsiFileForSolution(project: Project, language: Language): PsiFile = PsiFileFactory.getInstance(project).createFileFromText(
  "solution", language, this
)

fun applyInspections(code: String, project: Project, language: Language): String {
  val course = project.course ?: return code
  val inspectionIds = EduAIHintsProcessor.forCourse(course)?.getInspectionsProvider()?.inspectionIds ?: return code
  val inspections = getInspections(language, inspectionIds)
  val psiFile = runReadAction { PsiFileFactory.getInstance(project).createFileFromText("file", language, code) }
  for (inspection in inspections) {
    psiFile.applyLocalInspection(inspection).forEach { descriptor ->
      descriptor.fixes?.firstOrNull()?.let { quickFix ->
        WriteCommandAction.runWriteCommandAction(project, null, null, {
          quickFix.applyFix(project, descriptor)
        }, psiFile)
      }
    }
  }
  return runReadAction<String> { psiFile.text }
}

private fun getInspections(language: Language, inspections: Set<String>): List<LocalInspectionTool> {
  return LocalInspectionEP.LOCAL_INSPECTION.extensions
    .filter { it.language == language.id }
    .mapNotNull { it.instantiateTool().asSafely<LocalInspectionTool>() }
    .filter { it.id in inspections }
}

private fun PsiFile.applyLocalInspection(inspection: LocalInspectionTool): List<ProblemDescriptor> {
  val problems = mutableListOf<ProblemDescriptor>()
  val inspectionManager = InspectionManager.getInstance(project)
  ProgressManager.getInstance().executeProcessUnderProgress(
    {
      problems.addAll(
        runReadAction<List<ProblemDescriptor>> {
          inspection.processFile(this, inspectionManager)
        })
    },
    DaemonProgressIndicator()
  )
  return problems
}