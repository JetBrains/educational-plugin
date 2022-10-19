package com.jetbrains.edu.learning.taskDescription.ui.uihtml

import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers.JcefUIHtmlViewer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers.SwingUIHtmlViewer
import javax.swing.JTextPane

data class HtmlTransformerContext(val project: Project, val task: Task?)

interface SwingHtmlTransformer {

  fun swingTransform(html: String, context: HtmlTransformerContext): String

  fun setupSwingPanel(project: Project, htmlViewer: SwingUIHtmlViewer) {}
}

interface JcefHtmlTransformer {

  fun jcefTransform(html: String, context: HtmlTransformerContext): String

  fun setupJcefPanel(project: Project, htmlViewer: JcefUIHtmlViewer) {}
}

/**
 * Html transformers take some html as an input, and output another html.
 * The input html is usually authored by a teacher.
 * And the output html is intended to be used inside UI components, such as [JTextPane], [JCEFHtmlPanel].
 *
 * Transformers also set up UI components [JTextPane], [JCEFHtmlPanel] before usage, adding appropriate listeners and handlers to them.
 *
 * Html transformers during transformation take the Context into account.
 * The context consists of the project and the task.
 * It is needed, for example, to resolve paths inside a description according to a task folder.
 * It also allows for handling links according to a currently selected task.
 */
interface HtmlTransformer : SwingHtmlTransformer, JcefHtmlTransformer {

  infix fun then(that: HtmlTransformer): HtmlTransformer {
    val self = this

    return object : HtmlTransformer {
      override fun swingTransform(html: String, context: HtmlTransformerContext): String {
        val firstStep = self.swingTransform(html, context)
        return that.swingTransform(firstStep, context)
      }

      override fun setupSwingPanel(project: Project, htmlViewer: SwingUIHtmlViewer) {
        self.setupSwingPanel(project, htmlViewer)
        that.setupSwingPanel(project, htmlViewer)
      }

      override fun jcefTransform(html: String, context: HtmlTransformerContext): String {
        val firstStep = self.jcefTransform(html, context)
        return that.jcefTransform(firstStep, context)
      }

      override fun setupJcefPanel(project: Project, htmlViewer: JcefUIHtmlViewer) {
        self.setupJcefPanel(project, htmlViewer)
        that.setupJcefPanel(project, htmlViewer)
      }
    }
  }
}

/**
 * This is an html transformer that may be turned on or off with the [enabled] field.
 */
class SwitchableHtmlTransformer(private val htmlTransformer: HtmlTransformer, var enabled: Boolean = true) : HtmlTransformer {
  override fun swingTransform(html: String, context: HtmlTransformerContext): String =
    if (enabled) {
      htmlTransformer.swingTransform(html, context)
    }
    else {
      html
    }

  override fun jcefTransform(html: String, context: HtmlTransformerContext): String =
    if (enabled) {
      htmlTransformer.jcefTransform(html, context)
    }
    else {
      html
    }

  override fun setupSwingPanel(project: Project, htmlViewer: SwingUIHtmlViewer) {
    htmlTransformer.setupSwingPanel(project, htmlViewer)
  }

  override fun setupJcefPanel(project: Project, htmlViewer: JcefUIHtmlViewer) {
    htmlTransformer.setupJcefPanel(project, htmlViewer)
  }
}

/**
 * Makes transformations independently of the ui mode: jcef or swing
 */
abstract class UIModeIndependentHtmlTransformer : HtmlTransformer {
  override fun swingTransform(html: String, context: HtmlTransformerContext): String = transform(html, context)

  override fun jcefTransform(html: String, context: HtmlTransformerContext): String = transform(html, context)

  abstract fun transform(html: String, context: HtmlTransformerContext): String
}