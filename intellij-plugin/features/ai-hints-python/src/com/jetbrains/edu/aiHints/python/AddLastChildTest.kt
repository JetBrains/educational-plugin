package com.jetbrains.edu.aiHints.python

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyStatement

class AddLastChildTest : DumbAwareAction("Add Last Child") {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val psiFile = project.selectedTaskFile?.getVirtualFile(project)?.let { PsiManager.getInstance(project).findFile(it) } ?: return
    val newPsiFile = runReadAction { PsiFileFactory.getInstance(project).createFileFromText("codePsiFile", PythonLanguage.INSTANCE, psiFile.text) }
    val pyFunction = PsiTreeUtil.findChildrenOfType(newPsiFile, PyFunction::class.java).firstOrNull() ?: return // no copy
    val statementList = pyFunction.statementList
    val generator = PyElementGenerator.getInstance(project)
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      PsiDocumentManager.getInstance(project).commitAllDocuments()
      val returnStatement = generator.createFromText(LanguageLevel.getDefault(), PyStatement::class.java, "return result")
      statementList.add(returnStatement)
    })
    val documentManager = PsiDocumentManager.getInstance(project)
    documentManager.getDocument(pyFunction.containingFile)?.let {
      documentManager.doPostponedOperationsAndUnblockDocument(it)
    }
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      val document = project.selectedTaskFile?.getVirtualFile(project)?.document ?: error("VirtualFile for ${project.selectedTaskFile?.name} is null")
      document.setText(newPsiFile.text)
      documentManager.commitDocument(document)
    })
  }
}