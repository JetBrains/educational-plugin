package com.jetbrains.edu.scala.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.configuration.MainFileProvider
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject

class ScalaMainFileProvider : MainFileProvider {
  override fun findMainClass(project: Project, file: VirtualFile): String? {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null
    if (psiFile !is ScalaFile) return null

    PsiTreeUtil.findChildrenOfType(psiFile, ScObject::class.java).forEach {
      val mainMethod = findMainMethod(it)
      if (mainMethod.isDefined) return mainMethod.get().name
    }
    return null
  }
}