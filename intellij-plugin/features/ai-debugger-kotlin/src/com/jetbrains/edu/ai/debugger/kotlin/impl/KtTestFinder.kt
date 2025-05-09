package com.jetbrains.edu.ai.debugger.kotlin.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.ai.debugger.core.api.TestFinder
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction

class KtTestFinder : TestFinder() {
  override fun findTestByName(project: Project, testFiles: List<VirtualFile>, testName: String): String? {
    val (className, methodName) = testName.split(":")
    val psiManager = PsiManager.getInstance(project)
    return testFiles.asSequence()
      .mapNotNull { psiManager.findFile(it) }
      .flatMap { PsiTreeUtil.findChildrenOfType(it, KtClass::class.java) }
      .firstOrNull { it.fqName?.asString() == className }
      ?.declarations
      ?.filterIsInstance<KtNamedFunction>()
      ?.firstOrNull { it.name == methodName }
      ?.text
  }
}
