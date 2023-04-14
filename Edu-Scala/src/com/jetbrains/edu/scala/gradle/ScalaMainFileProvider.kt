package com.jetbrains.edu.scala.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.jvm.MainFileProvider
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.runner.MyScalaMainMethodUtil

class ScalaMainFileProvider : MainFileProvider {
  override fun findMainClassName(project: Project, file: VirtualFile): String? {
    val psiClass = findMainPsi(project, file) ?: return null
    return psiClass.name
  }

  override fun findMainPsi(project: Project, file: VirtualFile): PsiClass? {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null
    if (psiFile !is ScalaFile) return null

    return PsiTreeUtil.findChildrenOfType(psiFile, ScObject::class.java)
      .firstOrNull {
        MyScalaMainMethodUtil.findContainingMainMethod(psiFile).isDefined
      }?.fakeCompanionClassOrCompanionClass()
  }
}
