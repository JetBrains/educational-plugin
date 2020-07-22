package com.jetbrains.edu.learning.handlers.rename

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDirectoryContainer
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem

abstract class EduStudyItemRenameProcessor : RenamePsiFileProcessor() {

  override fun canProcessElement(element: PsiElement): Boolean {
    if (!(element is PsiDirectory || element is PsiDirectoryContainer)) return false
    val project = element.project
    val course = project.course ?: return false
    val file = getDirectory(element.toPsiDirectory(), course).virtualFile
    return getStudyItem(project, course, file) != null
  }

  override fun createRenameDialog(project: Project, element: PsiElement, nameSuggestionContext: PsiElement?, editor: Editor?): RenameDialog {
    val course = project.course ?: return super.createRenameDialog(project, element, nameSuggestionContext, editor)
    val file = getDirectory(element.toPsiDirectory(), course).virtualFile
    val item = getStudyItem(project, course, file) ?: return super.createRenameDialog(project, element, nameSuggestionContext, editor)

    return StudyItemRenameDialog(item, project, element, nameSuggestionContext, editor)
  }

  protected open fun getDirectory(element: PsiElement, course: Course): PsiDirectory = element.toPsiDirectory()

  protected abstract fun getStudyItem(project: Project, course: Course, file: VirtualFile): StudyItem?

  protected fun PsiElement.toPsiDirectory(): PsiDirectory {
    return (this as? PsiDirectoryContainer)?.directories?.first() ?: (this as PsiDirectory)
  }
}
