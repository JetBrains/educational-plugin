package com.jetbrains.edu.java.actions

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.usageView.UsageInfo
import com.jetbrains.edu.learning.handlers.isRenameForbidden
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.threadLocal

class JClassRenameProcessor : RenamePsiElementProcessor() {

  override fun canProcessElement(element: PsiElement): Boolean {
    if (JClassRenameService.getInstance().ignoreEduRenameProcessors) return false
    val adjustedElement = adjustElement(element) ?: return false
    // We want to handle rename refactoring ONLY when rename should be forbidden
    // to show the corresponding dialog in `renameElement` and forbid renaming
    if (!isRenameForbidden(element.project, adjustedElement)) return false
    // Do not try to handle rename refactoring if there aren't other rename processors besides this one
    return withoutEduRenameProcessors {
      EP_NAME.extensionList.any { it.canProcessElement(element) }
    }
  }

  override fun renameElement(element: PsiElement, newName: String, usages: Array<out UsageInfo>, listener: RefactoringElementListener?) {
    CommonRefactoringUtil.showErrorHint(
      element.project,
      null,
      EduCoreBundle.message("error.invalid.rename.message"),
      RefactoringBundle.message("rename.title"),
      null
    )
  }

  private fun <T> withoutEduRenameProcessors(action: () -> T): T {
    val service = JClassRenameService.getInstance()
    return if (service.ignoreEduRenameProcessors) {
      action()
    }
    else {
      service.ignoreEduRenameProcessors = true
      try {
        action()
      }
      finally {
        service.ignoreEduRenameProcessors = false
      }
    }
  }
}

@Service
class JClassRenameService {

  var ignoreEduRenameProcessors by threadLocal { false }

  companion object {
    fun getInstance(): JClassRenameService = service()
  }
}
