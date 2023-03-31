package com.jetbrains.edu.python.learning

import com.intellij.psi.PsiElement
import com.intellij.psi.util.QualifiedName
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.python.psi.impl.PyImportResolver
import com.jetbrains.python.psi.resolve.PyQualifiedNameResolveContext

class PyEduImportResolver : PyImportResolver {
  override fun resolveImportReference(
    name: QualifiedName,
    context: PyQualifiedNameResolveContext,
    withRoots: Boolean
  ): PsiElement? {
    if (StudyTaskManager.getInstance(context.project).course == null) {
      return null
    }
    val nameString = name.toString()
    val containingFile = context.footholdFile ?: return null
    val directory = containingFile.containingDirectory ?: return null
    return directory.findFile("$nameString.py")
  }
}
