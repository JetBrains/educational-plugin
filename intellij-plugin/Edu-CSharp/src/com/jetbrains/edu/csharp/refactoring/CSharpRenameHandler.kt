package com.jetbrains.edu.csharp.refactoring

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.PsiElementRenameHandler
import com.intellij.refactoring.rename.RenameHandler
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import com.jetbrains.edu.csharp.CSharpBackendService
import com.jetbrains.edu.learning.getLesson
import com.jetbrains.edu.learning.getSection
import com.jetbrains.edu.learning.getTask
import com.jetbrains.edu.learning.handlers.rename.LessonRenameProcessor
import com.jetbrains.edu.learning.handlers.rename.SectionRenameProcessor
import com.jetbrains.edu.learning.handlers.rename.TaskRenameProcessor
import com.jetbrains.rd.platform.util.project
import com.jetbrains.rdclient.util.idea.toIOFile
import com.jetbrains.rider.model.riderSolutionLifecycle
import com.jetbrains.rider.projectView.nodes.getVirtualFile
import com.jetbrains.rider.projectView.solution

class CSharpRenameHandler : RenameHandler {
  override fun invoke(p0: Project, p1: Editor?, p2: PsiFile?, p3: DataContext?) {
  }

  override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
    val element = elements.firstOrNull() ?: return
    val file = dataContext?.getVirtualFile() ?: return
    val processor = when {
      file.getTask(project) != null -> TaskRenameProcessor()
      file.getLesson(project) != null -> LessonRenameProcessor()
      file.getSection(project) != null -> SectionRenameProcessor()
      else -> RenamePsiFileProcessor()
    }
    if (file.parent.path == project.basePath) {
      CSharpBackendService.getInstance(project).stopIndexingTopLevelFiles(listOf(file.toIOFile()))
    }
    PsiElementRenameHandler.rename(
      element, project, element, CommonDataKeys.EDITOR.getData(dataContext),
      dataContext.getData(PsiElementRenameHandler.DEFAULT_NAME), processor
    )
  }

  override fun isAvailableOnDataContext(p0: DataContext): Boolean {
    return p0.project?.solution?.riderSolutionLifecycle?.isProjectModelReady?.valueOrNull ?: false
  }
}