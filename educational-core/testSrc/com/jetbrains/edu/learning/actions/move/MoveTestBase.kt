package com.jetbrains.edu.learning.actions.move

import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.refactoring.actions.MoveAction
import com.intellij.testFramework.MapDataContext
import com.jetbrains.edu.coursecreator.handlers.MoveStudyItemUI
import com.jetbrains.edu.coursecreator.handlers.withMockMoveStudyItemUI
import com.jetbrains.edu.learning.EduActionTestCase

abstract class MoveTestBase : EduActionTestCase() {

  protected fun doMoveAction(sourceDir: PsiDirectory, targetDir: PsiDirectory, delta: Int? = null) {
    val dataContext = dataContext(sourceDir).withTarget(targetDir)
    withMockMoveStudyItemUI(object : MoveStudyItemUI {
      override fun showDialog(project: Project, itemName: String, thresholdName: String): Int = delta ?: error("Pass `delta` value explicitly")
    }) {
      testAction(dataContext, MoveAction())
    }
  }

  protected fun findPsiDirectory(path: String): PsiDirectory {
    val file = findFile(path)
    return PsiManager.getInstance(project).findDirectory(file) ?: error("Failed to find directory for `$file` file")
  }

  private fun MapDataContext.withTarget(element: PsiElement): MapDataContext = apply { put(LangDataKeys.TARGET_PSI_ELEMENT, element) }
}
