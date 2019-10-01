package com.jetbrains.edu.coursecreator.handlers

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.refactoring.move.MoveHandlerDelegate
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.StudyItem

abstract class CCStudyItemMoveHandlerDelegate(private val itemName: String) : MoveHandlerDelegate() {

  override fun canMove(dataContext: DataContext): Boolean {
    val directory = CommonDataKeys.PSI_ELEMENT.getData(dataContext) as? PsiDirectory ?: return false
    return isAvailable(directory)
  }

  override fun canMove(elements: Array<PsiElement>, targetContainer: PsiElement?): Boolean {
    val directory = elements.singleOrNull() as? PsiDirectory ?: return false
    return isAvailable(directory)
  }

  override fun isValidTarget(psiElement: PsiElement?, sources: Array<PsiElement>): Boolean = true

  protected open fun getDelta(project: Project, targetItem: StudyItem): Int {
    return showMoveStudyItemDialog(project, itemName, targetItem.name)
  }

  override fun tryToMove(
    element: PsiElement,
    project: Project,
    dataContext: DataContext,
    reference: PsiReference?,
    editor: Editor?
  ): Boolean = CCUtils.isCourseCreator(project)

  protected abstract fun isAvailable(directory: PsiDirectory): Boolean
}
