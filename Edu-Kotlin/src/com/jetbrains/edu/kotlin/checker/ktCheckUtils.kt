package com.jetbrains.edu.kotlin.checker

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.EduUtils
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer
import org.jetbrains.kotlin.psi.KtElement

fun kotlinMainClassName(project: Project): String? = runReadAction {
  val editor = EduUtils.getSelectedEditor(project) ?: return@runReadAction null
  val virtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return@runReadAction null
  val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@runReadAction null

  val ktElements = PsiTreeUtil.findChildrenOfType(psiFile, KtElement::class.java)
  val container = KotlinRunConfigurationProducer.getEntryPointContainer(ktElements.first())
  KotlinRunConfigurationProducer.getStartClassFqName(container)
}
