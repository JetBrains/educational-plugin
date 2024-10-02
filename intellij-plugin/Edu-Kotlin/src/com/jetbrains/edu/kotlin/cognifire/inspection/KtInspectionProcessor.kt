package com.jetbrains.edu.kotlin.cognifire.inspection

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.edu.cognifire.inspection.InspectionProcessor
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.InspectionLikeProcessing
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

/**
 * A processor for applying inspections to Kotlin PSI files.
 * Only LiftReturnOrAssignment Inspection is supported. TODO(support more inspections)
 */
class KtInspectionProcessor : InspectionProcessor {

  override fun applyInspections(project: Project, psiFile: PsiFile) {
    psiFile.accept(object : PsiRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement) {
        when (element) {
          is KtWhenExpression, is KtIfExpression, is KtTryExpression -> {
            listOf(LiftReturnInspectionBasedProcessing(), LiftAssignmentInspectionBasedProcessing()).forEach {
              applyProcessingIfApplicable(project, element, it)
            }
          }
        }
        super.visitElement(element)
      }
    })
  }

  private fun applyProcessingIfApplicable(project: Project, element: KtElement, processing: InspectionLikeProcessing) {
    if (processing.isApplicableToElement(element, ConverterSettings.defaultSettings)) {
      WriteCommandAction.runWriteCommandAction(project, null, null, {
        processing.applyToElement(element)
      })
    }
  }
}
