package com.jetbrains.edu.learning.handlers.rename

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class MockRenameDialogFactory(
  private val newItemName: String
) : RenameDialogFactory {

  var delegate: RenameDialogFactory? = null

  var isDialogShown: Boolean = false
  var isRenameInvoked: Boolean = false

  override fun createRenameDialog(
    project: Project,
    element: PsiElement,
    nameSuggestionContext: PsiElement?,
    editor: Editor?
  ): EduRenameDialogBase {
    val delegateDialog = delegate!!.createRenameDialog(project, element, nameSuggestionContext, editor)
    val mockDialog = MockRenameDialog(this, delegateDialog, project, element, nameSuggestionContext, editor)
    mockDialog.setMockNewName(newItemName)
    return mockDialog
  }
}

class MockRenameDialog(
  private val factory: MockRenameDialogFactory,
  private val delegate: EduRenameDialogBase,
  project: Project,
  element: PsiElement,
  nameSuggestionContext: PsiElement?,
  editor: Editor?
) : EduRenameDialogBase(project, element, nameSuggestionContext, editor) {

  override fun setMockNewName(newName: String) {
    super.setMockNewName(newName)
    delegate.setMockNewName(newName)
  }

  override fun performRename(newName: String) {
    factory.isDialogShown = true
    try {
      // Unfortunately, rename handler doesn't check if rename action is available in tests
      // so let's do it ourselves
      delegate.canRun()
      factory.isRenameInvoked = true
      delegate.performRename(newName)
    }
    catch (e: ConfigurationException) {}
  }
}
