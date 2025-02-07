package com.jetbrains.edu.ai.error.explanation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.lang.Language
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.ComponentInlayAlignment
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayProperties
import com.intellij.openapi.editor.addComponentInlay
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.ai.clippy.assistant.AIClippyService
import com.jetbrains.edu.ai.clippy.assistant.AIClippyService.ClippyLinkAction
import com.jetbrains.edu.ai.error.explanation.grazie.ErrorExplanationGrazieClient
import com.jetbrains.edu.ai.error.explanation.messages.EduAIErrorExplanationBundle
import com.jetbrains.edu.ai.error.explanation.prompts.ErrorExplanationContext
import com.jetbrains.edu.ai.learner.feedback.AILearnerFeedbackService
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.ui.EduColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Font
import java.io.File

@Service(Service.Level.PROJECT)
class ErrorExplanationManager(private val project: Project, private val scope: CoroutineScope) {
  fun getErrorExplanation(language: Language, stderr: String) {
    LOG.info("STDERR:\n$stderr\n")
    val stackTrace = getFileAndLineNumber(language, stderr)
    if (stackTrace == null) {
      LOG.info("Found no line")
      return
    }
    if (language.id != EduFormatNames.PYTHON) return
    scope.launch {
      withBackgroundProgress(project, EduAIErrorExplanationBundle.message("error.explanation.fetching.error.explanation")) {
        val (fileName, lineNumber) = stackTrace
        val vfsFile = VfsUtil.findFileByIoFile(File(fileName), true) ?: return@withBackgroundProgress
        val document = runReadAction { FileDocumentManager.getInstance().getDocument(vfsFile) } ?: return@withBackgroundProgress
        val text = document.text
        val programmingLanguage = project.course?.languageById?.displayName ?: return@withBackgroundProgress
        val context = ErrorExplanationContext(programmingLanguage, text, stderr)

        val result = ErrorExplanationGrazieClient.getErrorExplanation(context)
        val errorExplanation = objectMapper.readValue(result, ErrorExplanation::class.java)
        LOG.info("RESULT:\n$result\n")

        withContext(Dispatchers.EDT) {
          openEditor(vfsFile, lineNumber, errorExplanation)
          showNotification(project, errorExplanation)
        }
      }
    }
  }

  fun showErrorExplanationPanelInClippy() {
    scope.launch {
      val feedback: String = AILearnerFeedbackService.getInstance(project).getFeedback(positive = false)

      val language = project.course?.languageById ?: return@launch
      val stdErr = ErrorExplanationStderrStorage.getInstance(project).getStderr() ?: return@launch

      val clippyLinkAction = ClippyLinkAction(EduAIErrorExplanationBundle.message("action.Educational.Student.ShowErrorExplanation.text")) { getErrorExplanation(language, stdErr) }

      AIClippyService.getInstance(project).showWithTextAndLinks(feedback, listOf(clippyLinkAction))
    }
  }

  private var prevRangeHighlighter: Pair<VirtualFile, RangeHighlighter>? = null
  private var prevInlay: Inlay<*>? = null

  private fun getFileAndLineNumber(language: Language, stderr: String): Pair<String, Int>? {
    val analyzer = ErrorAnalyzer.getInstance(language) ?: return null
    val stackTrace = analyzer.getStackTrace(stderr)
    val errorLine = stackTrace.findLast { (fileName, lineNumber) -> isCourseFile(fileName) && lineNumber > 0}
    return errorLine?.let { (fileName, lineNumber) -> Pair(fileName, lineNumber) }
  }

  private fun openEditor(vfsFile: VirtualFile, lineNumber: Int, errorExplanation: ErrorExplanation) {
    removePrevHighlighter(project)
    val descriptor = OpenFileDescriptor(project, vfsFile, lineNumber, 0)
    val fileEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true) ?: return
    val document = FileDocumentManager.getInstance().getDocument(vfsFile) ?: return
    val inlay = fileEditor.addComponentInlay(
      document.getLineEndOffset(lineNumber - 1),
      InlayProperties().showAbove(false).disableSoftWrapping(false),
      ErrorEditorPanel(errorExplanation.explanation) {
        removePrevHighlighter(project)
        fileEditor.component.repaint()
      },
      ComponentInlayAlignment.FIT_VIEWPORT_WIDTH
    )

    prevInlay = inlay
    descriptor.navigateInEditor(project, true)
    fileEditor.markupModel.removeAllHighlighters()
    val attributes = TextAttributes(null, EduColors.aiGetHintHighlighterColor, null, EffectType.BOXED, Font.PLAIN)
    val newRangeHighlighter = fileEditor.markupModel.addLineHighlighter(lineNumber - 1, 0, attributes)
    prevRangeHighlighter = vfsFile to newRangeHighlighter
  }

  private fun removePrevHighlighter(project: Project) {
    val (prevFile, prevRangeHighlighter) = prevRangeHighlighter ?: return
    val descriptor = OpenFileDescriptor(project, prevFile, 0, 0)
    val fileEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true) ?: return
    fileEditor.markupModel.removeHighlighter(prevRangeHighlighter)
    prevRangeHighlighter.dispose()
    prevInlay?.let { Disposer.dispose(it) }
    this.prevRangeHighlighter = null
    prevInlay = null
  }

  private fun showNotification(project: Project, errorExplanation: ErrorExplanation) {
    EduNotificationManager.showInfoNotification(
      project,
      EduAIErrorExplanationBundle.message("error.explanation"),
      errorExplanation.explanation
    )
  }

  private fun isCourseFile(fileName: String): Boolean {
    return try {
      val file = File(fileName)
      val virtualFile = VfsUtil.findFileByIoFile(file, true) ?: return false
      val taskFile = virtualFile.getTaskFile(project) ?: return false
      if (!taskFile.isVisible) return false
      true
    } catch (e: Exception) {
      false
    }
  }

  companion object {
    private val LOG = Logger.getInstance(ErrorExplanationStartupActivity::class.java)

    private val objectMapper = jacksonObjectMapper()

    fun getInstance(project: Project): ErrorExplanationManager = project.service()
  }
}