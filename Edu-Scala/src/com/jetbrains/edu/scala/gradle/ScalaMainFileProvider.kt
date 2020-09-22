package com.jetbrains.edu.scala.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.jvm.MainFileProvider
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.util.ScalaMainMethodUtil

class ScalaMainFileProvider : MainFileProvider {
  override fun findMainClass(project: Project, file: VirtualFile): String? {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null
    if (psiFile !is ScalaFile) return null

    PsiTreeUtil.findChildrenOfType(psiFile, ScObject::class.java).forEach {
      if (ScalaMainMethodUtil.findMainMethod(it).isDefined) return it.fakeCompanionClassOrCompanionClass().name
    }
    return null
  }
}
