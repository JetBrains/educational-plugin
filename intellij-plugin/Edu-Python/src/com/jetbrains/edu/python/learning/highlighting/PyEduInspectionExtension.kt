package com.jetbrains.edu.python.learning.highlighting

import com.intellij.psi.PsiReference
import com.intellij.psi.util.parentOfType
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.python.inspections.PyInspectionExtension
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyImportStatementBase
import com.jetbrains.python.psi.types.TypeEvalContext

class PyEduInspectionExtension : PyInspectionExtension() {
  override fun ignoreUnresolvedReference(element: PyElement, reference: PsiReference, context: TypeEvalContext): Boolean {
    val file = element.containingFile ?: return false
    val project = file.project
    if (project.course == null) {
      return false
    }
    val taskFile = file.virtualFile?.getTaskFile(project)
    if (taskFile == null || taskFile.errorHighlightLevel != EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION) {
      return false
    }
    return element.parentOfType<PyImportStatementBase>() == null
  }
}
