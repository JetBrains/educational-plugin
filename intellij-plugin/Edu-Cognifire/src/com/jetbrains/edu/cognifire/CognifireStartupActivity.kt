package com.jetbrains.edu.cognifire

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.cognifire.highlighting.prompttocode.PromptToCodeHighlighter
import com.jetbrains.edu.cognifire.parsers.ProdeExpressionParser
import com.jetbrains.edu.cognifire.highlighting.GuardedBlockManager
import com.jetbrains.edu.cognifire.highlighting.HighlighterManager
import com.jetbrains.edu.learning.selectedEditor

class CognifireStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    project.selectedEditor?.virtualFile?.let { addListenersToAllProdeInFile(it, project) }
    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        super.fileOpened(source, file)
        addListenersToAllProdeInFile(file, project)
      }
    })
  }

  private fun addListenersToAllProdeInFile(file: VirtualFile, project: Project) {
    val psiFile = runReadAction { PsiManager.getInstance(project).findFile(file) } ?: return
    val expressions = runReadAction { ProdeExpressionParser.getProdeExpressions(psiFile, psiFile.language) }
    invokeLater {
      expressions.forEach {
        val function = it.promptExpression.functionSignature
        val id = "${function.name}:${function.functionParameters.size}"
        PromptToCodeHighlighter(project, id).setUpDocumentListener(it.promptExpression, it.codeExpression)
        GuardedBlockManager.getInstance().addAllGuardBlocks(id, file)
        HighlighterManager.getInstance().highlightAllUncommitedChanges(id)
      }
    }
  }

}
