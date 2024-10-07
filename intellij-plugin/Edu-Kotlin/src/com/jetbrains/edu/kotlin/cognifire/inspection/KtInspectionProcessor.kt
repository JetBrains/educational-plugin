package com.jetbrains.edu.kotlin.cognifire.inspection

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.edu.cognifire.inspection.InspectionProcessor
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.inspections.FoldInitializerAndIfToElvisInspection
import org.jetbrains.kotlin.idea.inspections.branchedTransformations.IfThenToElvisInspection
import org.jetbrains.kotlin.idea.inspections.branchedTransformations.IfThenToSafeAccessInspection
import org.jetbrains.kotlin.idea.inspections.branchedTransformations.IntroduceWhenSubjectInspection
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.InspectionLikeProcessing
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

/**
 * A processor for applying inspections to Kotlin PSI files.
 * The following inspections are supported: LiftReturnOrAssignment, IntroduceWhenSubject, UnnecessaryVariableInspection, CascadeIf,
 * JoinDeclarationAndAssignment, FoldInitializerAndIfToElvis, ifThenToSafeAccess, IfThenToElvis. TODO(support more inspections)
 */
class KtInspectionProcessor : InspectionProcessor {

  override fun applyInspections(project: Project, psiFile: PsiFile) {
    psiFile.accept(object : PsiRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement) {
        when (element) {
          is KtWhenExpression, is KtIfExpression, is KtTryExpression -> {
            listOf(LiftReturnInspectionProcessing(), LiftAssignmentInspectionProcessing(), CascadeIfInspectionProcessing()).forEach {
              applyProcessingIfApplicable(project, element, it)
            }
            when (element) {
              is KtWhenExpression -> {
                listOf(IntroduceWhenSubjectInspection()).forEach {
                  applyAbstractApplicabilityBasedInspection(project, element, it)
                }
              }
              is KtIfExpression -> {
                listOf(IfThenToSafeAccessInspection(), IfThenToElvisInspection(), FoldInitializerAndIfToElvisInspection()).forEach {
                  applyAbstractApplicabilityBasedInspection(project, element, it)
                }
              }
            }
          }
          is KtProperty -> {
            listOf(JoinDeclarationAndAssignmentInspectionProcessing(), UnnecessaryVariableInspectionProcessing()).forEach {
              applyProcessingIfApplicable(project, element, it)
            }
          }
        }
        super.visitElement(element)
      }
    })
  }

  private fun applyProcessingIfApplicable(project: Project, element: PsiElement, processing: InspectionLikeProcessing) {
    if (processing.isApplicableToElement(element, ConverterSettings.defaultSettings)) {
      WriteCommandAction.runWriteCommandAction(project, null, null, {
        processing.applyToElement(element)
      })
    }
  }

  private fun <T : KtElement> applyAbstractApplicabilityBasedInspection(project: Project, element: T, inspection: AbstractApplicabilityBasedInspection<T>) {
    if (inspection.isApplicable(element)) {
      WriteCommandAction.runWriteCommandAction(project, null, null, {
        inspection.applyTo(element, project)
      })
    }
  }
}
