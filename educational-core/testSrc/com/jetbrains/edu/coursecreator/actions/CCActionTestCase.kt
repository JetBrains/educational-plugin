package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.TestActionEvent
import com.jetbrains.edu.learning.EduTestCase

abstract class CCActionTestCase : EduTestCase() {

  fun dataContext(files: Array<VirtualFile>): DataContext {
    return MapDataContext().apply {
      put(CommonDataKeys.PROJECT, project)
      put(CommonDataKeys.VIRTUAL_FILE_ARRAY, files)
    }
  }

  fun dataContext(file: VirtualFile): DataContext {
    val psiFile = PsiManager.getInstance(project).findDirectory(file)
    return MapDataContext().apply {
      put(CommonDataKeys.PROJECT, project)
      put(CommonDataKeys.VIRTUAL_FILE, file)
      put(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(file))
      put(CommonDataKeys.PSI_ELEMENT, psiFile)
    }
  }

  fun testAction(context: DataContext, action: AnAction) {
    val e = TestActionEvent(context, action)
    action.beforeActionPerformedUpdate(e)
    if (e.presentation.isEnabledAndVisible) {
      action.actionPerformed(e)
    }
  }

}
