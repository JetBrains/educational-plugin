package com.jetbrains.edu.learning.socialmedia.twitter.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.socialmedia.twitter.TwitterPluginConfigurator
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

class DefaultTwitterDialogPanel(
  configurator: TwitterPluginConfigurator,
  solvedTask: Task,
  imagePath: Path?,
  disposable: Disposable
) : TwitterDialogPanel(VerticalFlowLayout(0, 0)) {

  private val twitterTextField: JTextArea = JTextArea(4, 0)

  init {
    border = JBUI.Borders.empty()
    twitterTextField.lineWrap = true
    twitterTextField.text = configurator.getDefaultMessage(solvedTask)
    // JTextArea doesn't support scrolling itself
    val scrollPane = JBScrollPane(twitterTextField)
    add(scrollPane)

    if (imagePath != null) {
      val component = createImageComponent(imagePath, disposable)
      // Don't use border for component because it changes size of component content
      add(Box.createVerticalStrut(JBUI.scale(10)))
      add(component)
    }
  }

  override val message: String get() = twitterTextField.text

  override fun doValidate(): ValidationInfo? {
    val extraCharacters = twitterTextField.text.length - 280
    return if (extraCharacters > 0) {
      ValidationInfo(EduCoreBundle.message("twitter.validation.maximum.length", extraCharacters), twitterTextField)
    }
    else {
      super.doValidate()
    }
  }

  private fun createImageComponent(path: Path, disposable: Disposable): JComponent {
    val browser = TwitterJBCefBrowser(path)
    Disposer.register(disposable, browser)
    val component = browser.component
    component.preferredSize = calculateImageDimension(path)
    return component
  }

  private fun calculateImageDimension(path: Path): Dimension {
    // TODO: it can be done more efficiently
    val icon = ImageIcon(path.toString())
    return Dimension(icon.iconWidth, icon.iconHeight)
  }

  private class TwitterJBCefBrowser(path: Path) : JBCefBrowser(path.toUri().toString()) {
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
