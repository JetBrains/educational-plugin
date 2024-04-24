package com.jetbrains.edu.learning

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.jetbrains.edu.coursecreator.CCStudyItemDeleteProvider
import com.jetbrains.edu.learning.projectView.CourseViewPane

abstract class EduActionTestCase : EduTestCase() {

  protected fun dataContext(files: Array<VirtualFile>): DataContext {
    return SimpleDataContext.builder()
      .add(CommonDataKeys.PROJECT, project)
      .add(LangDataKeys.MODULE, myFixture.module)
      .add(CommonDataKeys.VIRTUAL_FILE_ARRAY, files)
      .build()
  }

  protected fun dataContext(file: VirtualFile): DataContext {
    val psiManager = PsiManager.getInstance(project)
    val psiFile = psiManager.findDirectory(file) ?: psiManager.findFile(file)
    val studyItem = file.getStudyItem(project)
    
    val builder = SimpleDataContext.builder()
      .add(CommonDataKeys.PROJECT, project)
      .add(LangDataKeys.MODULE, myFixture.module)
      .add(CommonDataKeys.VIRTUAL_FILE, file)
      .add(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(file))
      .add(CommonDataKeys.PSI_ELEMENT, psiFile)
      .add(PlatformDataKeys.DELETE_ELEMENT_PROVIDER, CCStudyItemDeleteProvider())
    if (psiFile is PsiFile) {
      builder.add(LangDataKeys.PSI_FILE, psiFile)
    }
    if (studyItem != null) {
      builder.add(CourseViewPane.STUDY_ITEM, studyItem)
    }
    return builder.build()
  }

  protected fun dataContext(element: PsiElement): DataContext {
    val file = if (element is PsiFileSystemItem) element.virtualFile else element.containingFile.virtualFile
    return SimpleDataContext.builder()
      .add(CommonDataKeys.PROJECT, project)
      .add(LangDataKeys.MODULE, myFixture.module)
      .add(CommonDataKeys.VIRTUAL_FILE, file)
      .add(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(file))
      .add(CommonDataKeys.PSI_ELEMENT, element)
      .add(PlatformDataKeys.DELETE_ELEMENT_PROVIDER, CCStudyItemDeleteProvider())
      .build()
  }

  companion object {
    @Suppress("HardCodedStringLiteral")
    fun simpleDiffRequestChain(
      project: Project?,
      currentContentText: String = "Current Content",
      anotherContentText: String = "Another Content"
    ): SimpleDiffRequestChain {
      val diffContentFactory = DiffContentFactory.getInstance()
      val currentContent = diffContentFactory.create(project, currentContentText)
      val anotherContent = diffContentFactory.create(project, anotherContentText)

      return SimpleDiffRequestChain(
        listOf(
          SimpleDiffRequest(
            "Simple Diff Request", currentContent, anotherContent, "Title: 1", "Title: 2"
          )
        )
      )
    }
  }
}
