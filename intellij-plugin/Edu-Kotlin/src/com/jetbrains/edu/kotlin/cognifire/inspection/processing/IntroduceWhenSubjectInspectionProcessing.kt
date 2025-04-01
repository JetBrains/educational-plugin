package com.jetbrains.edu.kotlin.cognifire.inspection.processing

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent
import org.jetbrains.kotlin.idea.inspections.branchedTransformations.IntroduceWhenSubjectInspection
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.scripting.definitions.runReadAction

class IntroduceWhenSubjectInspectionProcessing(private val project: Project, private val element: KtWhenExpression) :
  BaseInspectionProcessing(element) {
  private val inspection = IntroduceWhenSubjectInspection()

  override fun isApplicable(): Boolean = runReadAction {
    super.isApplicable()
    inspection.isApplicable(element)
  }

  override fun apply() {
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      inspection.applyTo(element, project)
    })
  }

  override fun applyInspection(promptToCode: PromptToCodeContent, psiFile: PsiFile): PromptToCodeContent {
    if (!isApplicable()) return promptToCode
    apply()
    return updatePromptToCodeWithoutChangingLines(promptToCode, psiFile)
  }
}
