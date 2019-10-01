package com.jetbrains.edu.learning.actions.rename

import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.refactoring.actions.RenameElementAction
import com.intellij.refactoring.rename.PsiElementRenameHandler
import com.intellij.refactoring.rename.RenameHandlerRegistry
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.MapDataContext
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import java.util.function.Function

abstract class RenameTestBase : EduActionTestCase() {

  private fun doRenameAction(course: Course, target: Any, newName: String, dialog: EduTestDialogBase<*>, shouldBeShown: Boolean = true) {
    val dataContext = when (target) {
      is String -> dataContext(findFile(target))
      is VirtualFile -> dataContext(target)
      is PsiElement -> dataContext(target)
      else -> error("Unexpected class of target: ${target.javaClass.name}. Only String, VirtualFile or PsiElement are supported")
    }

    withTestDialog(dialog) {
      withVirtualFileListener(course) {
        RenameHandlerRegistry.getInstance().setRenameHandlerSelectorInTests(Function { handlers ->
          error("Multiple rename handlers: ${handlers.map { it.javaClass.name }}")
        }, testRootDisposable)
        testAction(dataContext.withRenameDefaultName(newName), RenameElementAction())
      }
    }
    if (shouldBeShown) {
      dialog.checkWasShown()
    } else {
      check(dialog.shownMessage == null)
    }
  }

  protected fun doRenameAction(course: Course, target: Any, newName: String, shouldBeShown: Boolean = true) =
    doRenameAction(course, target, newName, EduTestDialog(), shouldBeShown)

  protected fun doRenameActionWithInput(course: Course, target: Any, newName: String, shouldBeShown: Boolean = true) =
    doRenameAction(course, target, newName, RenameTestInputDialog(newName), shouldBeShown)

  private fun MapDataContext.withRenameDefaultName(newName: String): MapDataContext =
    apply { put(PsiElementRenameHandler.DEFAULT_NAME, newName) }

  protected fun findDescriptionFile(name: String): VirtualFile? =
    LightPlatformTestCase.getSourceRoot().findFileByRelativePath("lesson1/task1/$name")

  protected class RenameTestInputDialog(private val message: String) : EduTestInputDialog(message) {

    override fun show(msg: String, validator: InputValidator?): String? =
      if (validator?.checkInput(message) == false) null else show(message)
  }
}
