package com.jetbrains.edu.learning.handlers.rename

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting

fun createRenameDialog(
  project: Project,
  element: PsiElement,
  nameSuggestionContext: PsiElement?,
  editor: Editor?,
  factory: RenameDialogFactory
): RenameDialog {
  val renameFactory = if (isUnitTestMode) {
    MOCK?.apply {
      delegate = factory
    } ?: error("Mock rename factory should be set via `withMockRenameDialogFactory`")

  }
  else {
    factory
  }
  return renameFactory.createRenameDialog(project, element, nameSuggestionContext, editor)
}

private var MOCK: MockRenameDialogFactory ? = null

@TestOnly
fun withMockRenameDialogFactory(mock: MockRenameDialogFactory, action: () -> Unit) {
  MOCK = mock
  try {
    action()
  }
  finally {
    MOCK = null
  }
}

interface RenameDialogFactory {
  fun createRenameDialog(
    project: Project,
    element: PsiElement,
    nameSuggestionContext: PsiElement?,
    editor: Editor?
  ): EduRenameDialogBase
}

abstract class EduRenameDialogBase(
  project: Project,
  element: PsiElement,
  nameSuggestionContext: PsiElement?,
  editor: Editor?
) : RenamePsiFileProcessor.PsiFileRenameDialog(project, element, nameSuggestionContext, editor) {

  private var mockNewName: String? = null

  @TestOnly
  open fun setMockNewName(newName: String) {
    mockNewName = newName
  }

  @VisibleForTesting
  public override fun canRun() {
    super.canRun()
  }

  override fun getSuggestedNames(): Array<String> {
    val newName = if (isUnitTestMode) mockNewName else null
    return newName?.let { arrayOf(it) } ?: super.getSuggestedNames()
  }

  override fun getNewName(): String {
    val newName = if (isUnitTestMode) mockNewName else null
    return newName ?: super.getNewName()
  }
}
