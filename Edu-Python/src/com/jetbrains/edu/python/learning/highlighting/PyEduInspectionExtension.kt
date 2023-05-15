package com.jetbrains.edu.python.learning.highlighting

import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.python.inspections.PyInspectionExtension
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyImportStatementBase
import com.jetbrains.python.psi.types.TypeEvalContext

class PyEduInspectionExtension : PyInspectionExtension() {
  override fun ignoreUnresolvedReference(element: PyElement, reference: PsiReference, context: TypeEvalContext): Boolean {
    val file = element.containingFile
    val project = file.project
    if (StudyTaskManager.getInstance(project).course == null) {
      return false
    }
    val taskFile = file.virtualFile.getTaskFile(project)
    if (taskFile == null || taskFile.errorHighlightLevel != EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION) {
      return false
    }
    return PsiTreeUtil.getParentOfType(element, PyImportStatementBase::class.java) == null
  }
}
