package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.AbstractPainter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeGlassPaneUtil
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.editor.EduSplitEditor
import com.jetbrains.edu.learning.handlers.AnswerPlaceholderDeleteHandler
import org.jetbrains.annotations.TestOnly
import java.awt.BasicStroke
import java.awt.Component
import java.awt.Graphics2D
import java.awt.Shape

object NewPlaceholderPainter {

  private val disposables: MutableMap<AnswerPlaceholder, MutableSet<Disposable>> = HashMap()

  @JvmOverloads
  @JvmStatic
  fun showPlaceholders(project: Project, taskFile: TaskFile, editor: Editor? = null) {
    if (project.isDisposed) return
    val editors = if (editor != null) listOf(editor) else taskFile.getEditors(project)
    for (placeholder in taskFile.answerPlaceholders) {
      showPlaceholder(project, placeholder, editors)
    }
  }

  @JvmOverloads
  @JvmStatic
  fun showPlaceholder(project: Project, placeholder: AnswerPlaceholder, editors: List<Editor>? = null) {
    if (project.isDisposed) return
    val taskFile = placeholder.taskFile ?: return

    @Suppress("NAME_SHADOWING")
    val editors = editors ?: taskFile.getEditors(project)

    for (editor in editors) {
      paintPlaceholder(project, taskFile, placeholder, editor)
    }
  }

  private fun paintPlaceholder(
    project: Project,
    taskFile: TaskFile,
    placeholder: AnswerPlaceholder,
    editor: Editor
  ) {
    val document = editor.document
    if (!taskFile.isValid(document.text)) return

    val isStudentProject = EduUtils.isStudentProject(project)
    val painter: AbstractPainter = object : AbstractPainter() {

      override fun needsRepaint() = !editor.isDisposed

      override fun executePaint(component: Component?, g: Graphics2D) {
        if (isStudentProject && !placeholder.isVisible) return
        g.color = placeholder.color
        g.stroke = BasicStroke(JBUI.scale(2f))
        val shape = getPlaceholderShape(editor, placeholder.offset, placeholder.endOffset).getShape()
        if (!isVisible(shape, editor)) return
        g.draw(shape)
      }
    }

    val placeholderDisposables = disposables.getOrPut(placeholder) { HashSet() }
    val painterDisposable = PainterDisposable(placeholder, editor)
    placeholderDisposables += painterDisposable
    IdeGlassPaneUtil.installPainter(editor.contentComponent, painter, painterDisposable)

    val handler = EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(document)
    if (handler !is AnswerPlaceholderDeleteHandler) {
      EditorActionManager.getInstance()
        .setReadonlyFragmentModificationHandler(document, AnswerPlaceholderDeleteHandler(editor))
    }
  }

  private fun TaskFile.getEditors(project: Project): List<Editor> {
    val file = getVirtualFile(project) ?: return emptyList()
    return FileEditorManager.getInstance(project)
      .allEditors
      .asSequence()
      .flatMap {
        if (it is EduSplitEditor) {
          sequenceOf(it.mainEditor, it.secondaryEditor)
        } else {
          sequenceOf(it)
        }
      }
      .filterIsInstance<TextEditor>()
      .filter { it.file == file }
      .map { it.editor }
      .toList()
  }

  private fun isVisible(shape: Shape, editor: Editor): Boolean {
    return editor.contentComponent.visibleRect.contains(shape.bounds)
  }

  @JvmStatic
  fun hidePlaceholder(placeholder: AnswerPlaceholder) {
    disposables.remove(placeholder).orEmpty().forEach(Disposer::dispose)
  }

  @JvmStatic
  fun hidePlaceholders(taskFile: TaskFile) {
    for (placeholder in taskFile.answerPlaceholders) {
      NewPlaceholderPainter.hidePlaceholder(placeholder)
    }
  }

  @TestOnly
  @JvmStatic
  fun getPaintedPlaceholder(): Set<AnswerPlaceholder> = disposables.keys

  private class PainterDisposable(private val placeholder: AnswerPlaceholder, editor: Editor) : Disposable {
    init {
      EditorUtil.disposeWithEditor(editor, this)
    }

    override fun dispose() {
      val placeholderDisposables = disposables[placeholder] ?: return
      placeholderDisposables.remove(this)
      if (placeholderDisposables.isEmpty()) {
        disposables.remove(placeholder)
      }
    }
  }
}
