package com.jetbrains.edu.kotlin.cognifire.inspection

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.edu.cognifire.inspection.InspectionProcessor
import com.jetbrains.edu.kotlin.cognifire.inspection.processing.*
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse
import org.jetbrains.kotlin.psi.*

/**
 * A processor for applying inspections to Kotlin PSI files.
 * The following inspections are supported: LiftReturnOrAssignment, IntroduceWhenSubject, CascadeIf,
 * JoinDeclarationAndAssignment, FoldInitializerAndIfToElvis, ifThenToSafeAccess, IfThenToElvis. TODO(support more inspections)
 */
class KtInspectionProcessor : InspectionProcessor {

  override fun applyInspections(promptToCodeTranslation: PromptToCodeResponse, project: Project, psiFile: PsiFile): PromptToCodeResponse {
    var promptToCode = promptToCodeTranslation.map { it.copy() }
    psiFile.accept(object : PsiRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement) {
        when (element) {
          is KtTryExpression -> {
            promptToCode = LiftReturnOrAssignmentInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
          }
          is KtWhenExpression -> {
            promptToCode = LiftReturnOrAssignmentInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
            promptToCode = IntroduceWhenSubjectInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
          }
          is KtIfExpression -> {
            promptToCode = CascadeIfInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
            promptToCode = LiftReturnOrAssignmentInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
            promptToCode = FoldInitializerAndIfToElvisInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
            promptToCode = IfThenToElvisInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
            promptToCode = IfThenToSafeAccessInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
          }
          is KtProperty -> {
            promptToCode = JoinDeclarationAndAssignmentInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
          }
        }
        super.visitElement(element)
      }
    })
    return promptToCode
  }
}
