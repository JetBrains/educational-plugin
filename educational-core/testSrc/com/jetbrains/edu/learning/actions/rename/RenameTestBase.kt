package com.jetbrains.edu.learning.actions.rename

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.refactoring.actions.RenameElementAction
import com.intellij.refactoring.rename.PsiElementRenameHandler
import com.intellij.refactoring.rename.RenameHandlerRegistry
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.MapDataContext
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.handlers.rename.MockRenameDialogFactory
import com.jetbrains.edu.learning.handlers.rename.withMockRenameDialogFactory
import java.util.function.Function

abstract class RenameTestBase : EduActionTestCase() {

  protected fun doRenameAction(
    course: Course,
    target: Any,
    newName: String,
    shouldBeShown: Boolean = true,
    shouldBeInvoked: Boolean = shouldBeShown
  ) {
    val dataContext = when (target) {
      is String -> dataContext(findFile(target))
      is VirtualFile -> dataContext(target)
      is PsiElement -> dataContext(target)
      else -> error("Unexpected class of target: ${target.javaClass.name}. Only String, VirtualFile or PsiElement are supported")
    }

    val factory = MockRenameDialogFactory(newName)

    withMockRenameDialogFactory(factory) {
      withVirtualFileListener(course) {
        RenameHandlerRegistry.getInstance().setRenameHandlerSelectorInTests(Function { handlers ->
          error("Multiple rename handlers: ${handlers.map { it.javaClass.name }}")
        }, testRootDisposable)
        testAction(dataContext.withRenameDefaultName(newName), IdeActions.ACTION_RENAME)
      }
    }

    assertTrue("Rename dialog is ${if (!shouldBeShown) "" else "not "}shown", shouldBeShown == factory.isDialogShown)
    assertTrue("Rename refactoring is ${if (!shouldBeInvoked) "" else "not "}invoked", shouldBeInvoked == factory.isRenameInvoked)
  }

  private fun MapDataContext.withRenameDefaultName(newName: String): MapDataContext =
    apply { put(PsiElementRenameHandler.DEFAULT_NAME, newName) }

  protected fun findDescriptionFile(name: String): VirtualFile? =
    LightPlatformTestCase.getSourceRoot().findFileByRelativePath("lesson1/task1/$name")
}
