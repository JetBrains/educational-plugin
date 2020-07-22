package com.jetbrains.edu.coursecreator.handlers.rename

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat

class CCDescriptionFileRenameProcessor : RenamePsiFileProcessor() {

  override fun canProcessElement(element: PsiElement): Boolean {
    if (element !is PsiFile) return false
    val project = element.project
    if (EduUtils.isStudentProject(project)) return false
    val file = element.virtualFile
    if (!EduUtils.isTaskDescriptionFile(file.name)) return false
    val task = EduUtils.getTaskForFile(project, file) ?: return false
    return file.parent == task.getTaskDir(project)
  }

  override fun createRenameDialog(project: Project, element: PsiElement, nameSuggestionContext: PsiElement?, editor: Editor?): RenameDialog {
    val task = EduUtils.getTaskForFile(project, (element as PsiFile).virtualFile)
               ?: return super.createRenameDialog(project, element, nameSuggestionContext, editor)

    return object : RenamePsiFileProcessor.PsiFileRenameDialog(project, element, nameSuggestionContext, editor) {

      init {
        title = "Rename description file"
      }

      override fun performRename(newName: String) {
        val format = DescriptionFormat.values().find { it.descriptionFileName == newName } ?: error("Unexpected new name: `$newName`")
        task.descriptionFormat = format
        super.performRename(newName)
      }

      @Throws(ConfigurationException::class)
      override fun canRun() {
        if (!EduUtils.isTaskDescriptionFile(newName)) {
          throw ConfigurationException("Description file should be named '${EduNames.TASK_HTML}' or '${EduNames.TASK_MD}'.")
        }
      }
    }
  }
}
