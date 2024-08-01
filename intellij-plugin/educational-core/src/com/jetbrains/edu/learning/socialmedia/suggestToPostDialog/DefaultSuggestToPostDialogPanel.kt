package com.jetbrains.edu.learning.socialmedia.suggestToPostDialog

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.socialmedia.SocialmediaPluginConfigurator
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import java.awt.Dimension
import java.nio.file.Path
import javax.swing.Box
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JTextArea
import kotlin.math.max

open class DefaultSuggestToPostDialogPanel(
  configurator: SocialmediaPluginConfigurator,
  solvedTask: Task,
  imagePath: Path?,
  disposable: Disposable
) : SuggestToPostDialogPanel(VerticalFlowLayout(0, 0)) {

  private val textField: JTextArea = JTextArea(4, 0)

  init {
    border = JBUI.Borders.empty()
    textField.lineWrap = true
    textField.text = configurator.getDefaultMessage(solvedTask)
    // JTextArea doesn't support scrolling itself
    val scrollPane = JBScrollPane(textField)
    add(scrollPane)

    if (imagePath != null) {
      val component = createImageComponent(imagePath, disposable)
      // Don't use border for component because it changes size of component content
      add(Box.createVerticalStrut(JBUI.scale(10)))
      add(component)
    }
  }

  override val message: String get() = textField.text

  private fun createImageComponent(path: Path, disposable: Disposable): JComponent {
    val browser = SuggestToPostJBCefBrowser(path)
    browser.cefBrowser.uiComponent.preferredSize = Dimension(600, 315)
    Disposer.register(disposable, browser)
    val component = browser.component
    component.maximumSize = calculateImageDimension(path)
    return component
  }

  private fun calculateImageDimension(path: Path): Dimension {
    // TODO: it can be done more efficiently
    val icon = ImageIcon(path.toString())
    return Dimension(max(icon.iconWidth, 600), max(icon.iconHeight, 315))
  }

  private class SuggestToPostJBCefBrowser(path: Path) : JBCefBrowser(path.toUri().toString()) {
    override fun createDefaultContextMenuHandler(): DefaultCefContextMenuHandler {
      val isInternal = ApplicationManager.getApplication().isInternal
      return object : DefaultCefContextMenuHandler(isInternal) {
        override fun onBeforeContextMenu(browser: CefBrowser, frame: CefFrame, params: CefContextMenuParams, model: CefMenuModel) {
          // Clear context menu
          // Inspired by `com.intellij.ui.jcef.JCEFHtmlPanel`
          model.clear()
          super.onBeforeContextMenu(browser, frame, params, model)
        }
      }
    }
  }
}
