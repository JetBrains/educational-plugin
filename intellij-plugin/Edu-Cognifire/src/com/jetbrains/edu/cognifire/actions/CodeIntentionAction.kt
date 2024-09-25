package com.jetbrains.edu.cognifire.actions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.cognifire.messages.EduCognifireBundle
import javax.swing.Icon
import com.jetbrains.edu.cognifire.CognifireDslPackageCallChecker
import com.jetbrains.edu.cognifire.CodeApplier
import com.jetbrains.edu.cognifire.utils.CODE

/**
 * Represents an intention action for `code` DSL elements.
 * The main purpose is to provide users with an option to apply the code enclosed within the `code` block, provided there are no associated errors within the block.
 *
 * This class implements the [IntentionAction] and [Iconable] interfaces.
 *
 * @see IntentionAction
 * @see Iconable
 */
class CodeIntentionAction : IntentionAction, Iconable {
  override fun startInWriteAction(): Boolean = false

  override fun getFamilyName(): String = EduCognifireBundle.message("action.code.intention.family.name")

  override fun getText(): String = EduCognifireBundle.message("action.code.intention.text")

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
    // TODO: verify that there are no errors and no internal blocks
    val caretModel: CaretModel = editor?.caretModel ?: return false
    val element: PsiElement = file?.findElementAt(caretModel.offset) ?: return false
    return element.text == CODE && CognifireDslPackageCallChecker.isCallFromCognifireDslPackage(element.parent.parent, element.language)
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    val caretModel: CaretModel = editor?.caretModel ?: return
    val element: PsiElement = file?.findElementAt(caretModel.offset) ?: return
    CodeApplier.applyDraftCodeToMainCode(project, element, file, element.language)
  }

  override fun getIcon(flags: Int): Icon = AllIcons.Actions.IntentionBulb
}
