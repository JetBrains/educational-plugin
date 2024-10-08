package com.jetbrains.edu.learning.socialmedia.suggestToPostDialog

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowser.DefaultCefContextMenuHandler
import com.intellij.util.ui.JBUI
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import java.awt.Dimension
import java.nio.file.Path
import javax.swing.Box
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.math.max

class SuggestToPostDialogPanel(
  message: String,
  imagePath: Path?,
  disposable: Disposable
) : JPanel(VerticalFlowLayout(0, 0)) {

  private val postText = JBTextArea()

  init {
    border = JBUI.Borders.empty()
    postText.text = message
    postText.border = JBUI.Borders.empty()
    postText.lineWrap = true
    postText.wrapStyleWord = true
    postText.isEditable = false

    add(postText)

    if (imagePath != null) {
      val component = createImageComponent(imagePath, disposable)
      // Don't use border for component because it changes size of component content
      add(Box.createVerticalStrut(JBUI.scale(10)))
      add(component)
    }

  }

  private fun createImageComponent(path: Path, disposable: Disposable): JComponent {
    val browser = SuggestToPostJBCefBrowser(path)
    browser.cefBrowser.uiComponent.preferredSize = Dimension(JBUI.scale(600), JBUI.scale(315))
    Disposer.register(disposable, browser)
    val component = browser.component
    component.maximumSize = calculateImageDimension(path)
    return component
  }

  private fun calculateImageDimension(path: Path): Dimension {
    val icon = ImageIcon(path.toString())
    val imageWidth = JBUI.scale(max(icon.iconWidth, 600))
    val imageHeight = JBUI.scale((max(icon.iconHeight, 315)))
    return Dimension(imageWidth, imageHeight)
  }

  fun doValidate(): ValidationInfo? {
    return null
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