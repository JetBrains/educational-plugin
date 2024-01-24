package com.jetbrains.edu.learning.placeholder

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.getTaskFile

class PlaceholderHighlightingPassFactory : TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar, DumbAware {

  override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
    if (!PlaceholderHighlightingManager.useNewRendering()) return null

    val taskFile = file.virtualFile?.getTaskFile(file.project) ?: return null

    return PlaceholderHighlightingPass(file, taskFile, editor.document)
  }

  override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
    registrar.registerTextEditorHighlightingPass(this, TextEditorHighlightingPassRegistrar.Anchor.FIRST, -1, false, false)
  }
}

class PlaceholderHighlightingPass(
  private val file: PsiFile,
  private val taskFile: TaskFile,
  document: Document
) : TextEditorHighlightingPass(file.project, document), DumbAware {

  private val results = mutableListOf<HighlightInfo>()

  override fun doCollectInformation(progress: ProgressIndicator) {
    val textLength = file.textLength
    for (placeholder in taskFile.answerPlaceholders) {
      progress.checkCanceled()

      if (!placeholder.isValid(textLength)) continue
      if (myProject.isStudentProject() && !placeholder.isCurrentlyVisible) continue
      val highlightInfo = PlaceholderHighlightingInfo.forStatus(placeholder.status)

      val highlightInfoBuilder = HighlightInfo.newHighlightInfo(highlightInfo.highlightInfoType)
        .range(placeholder.offset, placeholder.endOffset)
      if (placeholder.length == 0) {
        // Minor hack to show placeholders with zero length at all because
        // by default, highlighting for empty range is not rendered
        highlightInfoBuilder.endOfLine()
      }
      results += highlightInfoBuilder.createUnconditionally()
    }
  }

  override fun doApplyInformationToEditor() {
    UpdateHighlightersUtil.setHighlightersToEditor(
      myProject,
      myDocument,
      0,
      file.textLength,
      results,
      colorsScheme,
      id
    )
  }
}

enum class PlaceholderHighlightingInfo {
  UNCHECKED,
  SOLVED,
  FAILED;

  val severity: HighlightSeverity = HighlightSeverity("placeholder_${name.lowercase()}", HighlightSeverity.INFORMATION.myVal)
  val highlightInfoType: HighlightInfoType = HighlightInfoType.HighlightInfoTypeImpl(
    severity,
    TextAttributesKey.createTextAttributesKey("com.jetbrains.edu.PLACEHOLDER_$name")
  )

  companion object {
    fun forStatus(status: CheckStatus): PlaceholderHighlightingInfo {
      return when (status) {
        CheckStatus.Unchecked -> UNCHECKED
        CheckStatus.Solved -> SOLVED
        CheckStatus.Failed -> FAILED
      }
    }
  }
}