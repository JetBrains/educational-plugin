package com.jetbrains.edu.learning

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.testFramework.MapDataContext
import com.jetbrains.edu.coursecreator.CCStudyItemDeleteProvider
import com.jetbrains.edu.learning.projectView.CourseViewPane

abstract class EduActionTestCase : EduTestCase() {

  protected fun dataContext(files: Array<VirtualFile>): MapDataContext {
    return MapDataContext().apply {
      put(CommonDataKeys.PROJECT, project)
      put(LangDataKeys.MODULE, myFixture.module)
      put(CommonDataKeys.VIRTUAL_FILE_ARRAY, files)
    }
  }

  protected fun dataContext(file: VirtualFile): MapDataContext {
    val psiManager = PsiManager.getInstance(project)
    val psiFile = psiManager.findDirectory(file) ?: psiManager.findFile(file)
    val studyItem = file.getStudyItem(project)
    return MapDataContext().apply {
      put(CommonDataKeys.PROJECT, project)
      put(LangDataKeys.MODULE, myFixture.module)
      put(CommonDataKeys.VIRTUAL_FILE, file)
      put(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(file))
      if (psiFile is PsiFile) {
        put(LangDataKeys.PSI_FILE, psiFile)
      }
      put(CommonDataKeys.PSI_ELEMENT, psiFile)
      put(PlatformDataKeys.DELETE_ELEMENT_PROVIDER, CCStudyItemDeleteProvider())
      if (studyItem != null) {
        put(CourseViewPane.STUDY_ITEM, studyItem)
      }
    }
  }

  protected fun dataContext(element: PsiElement): MapDataContext {
    val file = if (element is PsiFileSystemItem) element.virtualFile else element.containingFile.virtualFile
    return MapDataContext().apply {
      put(CommonDataKeys.PROJECT, project)
      put(LangDataKeys.MODULE, myFixture.module)
      put(CommonDataKeys.VIRTUAL_FILE, file)
      put(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(file))
      put(CommonDataKeys.PSI_ELEMENT, element)
      put(PlatformDataKeys.DELETE_ELEMENT_PROVIDER, CCStudyItemDeleteProvider())
    }
  }
}
