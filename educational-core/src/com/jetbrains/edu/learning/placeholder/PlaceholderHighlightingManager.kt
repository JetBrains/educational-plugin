package com.jetbrains.edu.learning.placeholder

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.toPsiFile

object PlaceholderHighlightingManager {

  @JvmOverloads
  @JvmStatic
  fun showPlaceholders(project: Project, taskFile: TaskFile, editor: Editor? = null) {
    if (useNewRendering()) {
      val file = editor?.document?.toPsiFile(project) ?: taskFile.findPsiFile(project)
      if (file != null) {
        DaemonCodeAnalyzer.getInstance(project).restart(file)
      }
    }
    if (useOldRendering()) {
      PlaceholderPainter.showPlaceholders(project, taskFile, editor)
    }
  }

  @JvmStatic
  fun showPlaceholder(project: Project, placeholder: AnswerPlaceholder) {
    if (useNewRendering()) {
      val file = placeholder.findPsiFile(project)
      if (file != null) {
        DaemonCodeAnalyzer.getInstance(project).restart(file)
      }
    }
    if (useOldRendering()) {
      PlaceholderPainter.showPlaceholder(project, placeholder)
    }
  }

  @JvmStatic
  fun hidePlaceholders(project: Project, placeholders: List<AnswerPlaceholder>) {
    if (useNewRendering()) {
      val file = placeholders.firstOrNull()?.findPsiFile(project)
      if (file != null) {
        DaemonCodeAnalyzer.getInstance(project).restart(file)
      }
    }
    if (useOldRendering()) {
      PlaceholderPainter.hidePlaceholders(placeholders)
    }
  }

  @JvmStatic
  fun hidePlaceholder(project: Project, placeholder: AnswerPlaceholder) {
    if (useNewRendering()) {
      val file = placeholder.findPsiFile(project)
      if (file != null) {
        DaemonCodeAnalyzer.getInstance(project).restart(file)
      }
    }
    if (useOldRendering()) {
      PlaceholderPainter.hidePlaceholder(placeholder)
    }
  }

  private fun AnswerPlaceholder.findPsiFile(project: Project): PsiFile? = taskFile.findPsiFile(project)
  private fun TaskFile.findPsiFile(project: Project): PsiFile? {
    val file = getVirtualFile(project) ?: return null
    return PsiManager.getInstance(project).findFile(file)
  }

  // Render placeholders using both ways in tests
  private fun useOldRendering(): Boolean = isUnitTestMode || !Registry.`is`("edu.placeholder.highlighting.pass")
  fun useNewRendering(): Boolean = isUnitTestMode || Registry.`is`("edu.placeholder.highlighting.pass")
}
