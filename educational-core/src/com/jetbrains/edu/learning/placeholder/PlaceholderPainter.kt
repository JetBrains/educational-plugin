package com.jetbrains.edu.learning.placeholder

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
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.handlers.AnswerPlaceholderDeleteHandler
import org.jetbrains.annotations.TestOnly
import java.awt.*

object PlaceholderPainter {

  val STROKE_WIDTH: Float = JBUIScale.scale(2f)

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
    val taskFile = placeholder.taskFile

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
        g.color = placeholder.getColor()
        g.stroke = BasicStroke(STROKE_WIDTH)
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
      .filterIsInstance<TextEditor>()
      .filter { it.file == file }
      .map { it.editor }
  }
  private fun AnswerPlaceholder.getColor(): JBColor {
    return when (status) {
      CheckStatus.Solved -> {
        val colorLight = createColor("26993D", 90, JBColor.LIGHT_GRAY)
        val colorDark = createColor("47CC5E", 82, JBColor.LIGHT_GRAY)
        JBColor(colorLight, colorDark)
      }
      CheckStatus.Failed -> {
        val colorLight = createColor("CC0000", 64, JBColor.GRAY)
        val colorDark = createColor("FF7373", 90, JBColor.GRAY)
        JBColor(colorLight, colorDark)
      }
      else -> getDefaultPlaceholderColor()
    }
  }

  private fun getDefaultPlaceholderColor(): JBColor {
    val colorLight = createColor("284B73", 64, JBColor.GRAY)
    val colorDark = createColor("A1C1E6", 72, JBColor.GRAY)
    return JBColor(colorLight, colorDark)
  }

  private fun createColor(str: String, alpha: Int, defaultValue: Color): Color {
    val color = ColorUtil.fromHex(str, defaultValue)
    return ColorUtil.toAlpha(color, alpha)
  }

  private fun isVisible(shape: Shape, editor: Editor): Boolean {
    return editor.contentComponent.visibleRect.contains(shape.bounds)
  }

  @JvmStatic
  fun hidePlaceholder(placeholder: AnswerPlaceholder) {
    disposables.remove(placeholder).orEmpty().forEach(Disposer::dispose)
  }

  @JvmStatic
  fun hidePlaceholders(placeholders: List<AnswerPlaceholder>) {
    for (placeholder in placeholders) {
      hidePlaceholder(placeholder)
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
